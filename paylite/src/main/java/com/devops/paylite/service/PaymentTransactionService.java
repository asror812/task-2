package com.devops.paylite.service;

import com.devops.paylite.domain.Agent;
import com.devops.paylite.domain.Payment;
import com.devops.paylite.domain.enumeration.PaymentStatus;
import com.devops.paylite.repository.AgentRepository;
import com.devops.paylite.repository.PaymentRepository;
import com.devops.paylite.repository.PaymentOutboxEventRepository;
import com.devops.paylite.domain.PaymentOutboxEvent;
import com.devops.paylite.service.dto.payment.PaymentCreateRequestDto;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PaymentTransactionService {

    private final PaymentRepository paymentRepository;
    private final AgentRepository agentRepository;
    private final PaymentOutboxEventRepository outboxEventRepository;
    private final static Logger LOG = LoggerFactory.getLogger(PaymentTransactionService.class);

    @Value("${payment.max-attempts:4}")
    private int maxAttempts;

    @Transactional
    public Payment prepare(
        PaymentCreateRequestDto dto,
        String login,
        long commissionAmount,
        long totalAmount,
        String requestPayload
    ) {
        Agent agent = agentRepository
            .findByLoginForUpdate(login)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found"));

        Payment existing = paymentRepository.findByIdempotencyKey(dto.getIdempotencyKey()).orElse(null);
        if (existing != null) {
            validateReplay(existing, dto, login);
            return existing;
        }

        Instant now = Instant.now();
        PaymentStatus status = agent.getBalance() >= totalAmount ? PaymentStatus.PENDING : PaymentStatus.FAILED;

        if (status == PaymentStatus.PENDING) {
            agent.setBalance(agent.getBalance() - totalAmount);
            agentRepository.save(agent);
        }

        Payment payment = Payment.builder()
            .account(dto.getAccount())
            .amount(dto.getAmount())
            .totalAmount(totalAmount)
            .commissionAmount(commissionAmount)
            .checkId(dto.getCheckId())
            .recipientId(dto.getRecipientId())
            .requestPayload(requestPayload)
            .idempotencyKey(dto.getIdempotencyKey())
            .status(status)
            .agent(agent)
            .createdDate(now)
            .updatedDate(now)
            .attemptCount(0)
            .lastError(status == PaymentStatus.FAILED ? "Insufficient balance" : null)
            .build();

        Payment savedPayment = paymentRepository.saveAndFlush(payment);
        if (savedPayment.getStatus() == PaymentStatus.PENDING) {
            outboxEventRepository.save(PaymentOutboxEvent.paymentCreated(savedPayment.getId()));
        }
        return savedPayment;
    }

    @Transactional(readOnly = true)
    public Payment findWithAgent(Long paymentId) {
        return paymentRepository
            .findOneWithAgentById(paymentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
    }

    @Transactional(readOnly = true)
    public Payment findReplay(String idempotencyKey, PaymentCreateRequestDto dto, String login) {
        Payment payment = paymentRepository
            .findByIdempotencyKey(idempotencyKey)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.CONFLICT, "Concurrent payment creation failed"));

        validateReplay(payment, dto, login);
        return payment;
    }

    @Transactional
    public Optional<Payment> claimForProcessing(Long paymentId) {
        Payment payment = findPendingForUpdate(paymentId);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return Optional.empty();
        }

        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setAttemptCount(payment.getAttemptCount() + 1);
        payment.setUpdatedDate(Instant.now());
        payment.setLastError(null);
        return Optional.of(paymentRepository.save(payment));
    }

    @Transactional
    public Payment markPaid(Long paymentId, String paynetPaymentId) {
        Payment payment = findPendingForUpdate(paymentId);

        if (payment.getStatus() != PaymentStatus.PROCESSING) {
            return payment;
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaynetPaymentId(paynetPaymentId);
        payment.setUpdatedDate(Instant.now());
        payment.setLastError(null);
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment markPendingError(Long paymentId, String error) {
        Payment payment = findPendingForUpdate(paymentId);

        if (payment.getStatus() != PaymentStatus.PROCESSING) {
            return payment;
        }

        payment.setStatus(PaymentStatus.PENDING);
        payment.setUpdatedDate(Instant.now());
        payment.setLastError(limit(error));
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment markFailedAndReleaseBalance(Long paymentId, String error) {
        Payment payment = findPendingForUpdate(paymentId);

        if (payment.getStatus() != PaymentStatus.PROCESSING) {
            return payment;
        }

        Agent agent = agentRepository
            .findByIdForUpdate(payment.getAgent().getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found"));

        agent.setBalance(Math.addExact(agent.getBalance(), payment.getTotalAmount()));
        agentRepository.save(agent);

        payment.setStatus(PaymentStatus.FAILED);
        payment.setUpdatedDate(Instant.now());
        payment.setLastError(limit(error));
        return paymentRepository.save(payment);
    }

    @Transactional
    public void markReconciliationRequired(Long paymentId) {
        Payment payment = findPendingForUpdate(paymentId);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return;
        }

        payment.setStatus(PaymentStatus.RECONCILIATION_REQUIRED);
        payment.setUpdatedDate(Instant.now());
        payment.setLastError("Automatic payment attempts exhausted; manual reconciliation required");
        paymentRepository.save(payment);
    }

    @Transactional
    public int enqueueStalePaymentsForRecovery(Instant updatedBefore) {
        List<Payment> stalePayments = paymentRepository.findStaleForUpdate(
            List.of(PaymentStatus.PENDING, PaymentStatus.PROCESSING),
            updatedBefore,
            PageRequest.of(0, 50)
        );
        Instant now = Instant.now();
        stalePayments.forEach(payment -> {
            if (payment.getAttemptCount() >= maxAttempts) {
                payment.setStatus(PaymentStatus.RECONCILIATION_REQUIRED);
                payment.setUpdatedDate(now);
                payment.setLastError("Automatic payment attempts exhausted; manual reconciliation required");
                LOG.warn("Payment is being moved to the dead letter queue id={} attempts={}", payment.getId(), payment.getAttemptCount());
                return;
            }
            payment.setStatus(PaymentStatus.PENDING);
            payment.setUpdatedDate(now);
            payment.setLastError("Recovered by safety reconciliation");
            outboxEventRepository.save(PaymentOutboxEvent.paymentRecoveryRequested(payment.getId()));
        });
        paymentRepository.saveAll(stalePayments);
        return stalePayments.size();
    }

    private Payment findPendingForUpdate(Long paymentId) {
        return paymentRepository
            .findByIdForUpdate(paymentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
    }

    private void validateReplay(Payment payment, PaymentCreateRequestDto dto, String login) {
        if (payment.getAgent() == null || !login.equals(payment.getAgent().getLogin())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key already used");
        }

        if (
            !dto.getAmount().equals(payment.getAmount()) ||
            !dto.getAccount().equals(payment.getAccount()) ||
            !dto.getCheckId().equals(payment.getCheckId()) ||
            !dto.getRecipientId().equals(payment.getRecipientId())
        ) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key already used for another payment");
        }
    }

    private String limit(String error) {
        if (error == null || error.isBlank()) {
            return "Unknown payment processing error";
        }
        return error.length() <= 1000 ? error : error.substring(0, 1000);
    }
}
