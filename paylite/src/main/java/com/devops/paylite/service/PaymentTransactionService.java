package com.devops.paylite.service;

import com.devops.paylite.domain.Agent;
import com.devops.paylite.domain.Payment;
import com.devops.paylite.domain.enumeration.PaymentStatus;
import com.devops.paylite.repository.AgentRepository;
import com.devops.paylite.repository.PaymentRepository;
import com.devops.paylite.service.dto.payment.PaymentCreateRequestDto;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PaymentTransactionService {

    private final PaymentRepository paymentRepository;
    private final AgentRepository agentRepository;

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

        return paymentRepository.saveAndFlush(payment);
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
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Concurrent payment creation failed"));
        validateReplay(payment, dto, login);
        return payment;
    }

    @Transactional
    public Payment markAttempt(Long paymentId) {
        Payment payment = findPendingForUpdate(paymentId);
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return payment;
        }
        payment.setAttemptCount(payment.getAttemptCount() + 1);
        payment.setUpdatedDate(Instant.now());
        payment.setLastError(null);
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment markPaid(Long paymentId, String paynetPaymentId) {
        Payment payment = findPendingForUpdate(paymentId);
        if (payment.getStatus() != PaymentStatus.PENDING) {
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
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return payment;
        }
        payment.setUpdatedDate(Instant.now());
        payment.setLastError(limit(error));
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment markFailedAndReleaseBalance(Long paymentId, String error) {
        Payment payment = findPendingForUpdate(paymentId);
        if (payment.getStatus() != PaymentStatus.PENDING) {
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
