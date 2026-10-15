package com.devops.paylite.service;

import com.devops.paylite.config.PaymentProperties;
import com.devops.paylite.domain.Payment;
import com.devops.paylite.domain.PaynetRecipient;
import com.devops.paylite.domain.PaynetRecipientField;
import com.devops.paylite.domain.PaynetRecipientFieldValue;
import com.devops.paylite.domain.enumeration.PaymentStatus;
import com.devops.paylite.domain.enumeration.RecipientFieldMethod;
import com.devops.paylite.repository.PaymentRepository;
import com.devops.paylite.repository.PaynetRecipientRepository;
import com.devops.paylite.repository.PaynetRecipientFieldRepository;
import com.devops.paylite.security.SecurityUtils;
import com.devops.paylite.service.dto.paynet.PaynetCheckResponseDto;
import com.devops.paylite.service.dto.paynet.PaynetPayResponseDto;
import com.devops.paylite.service.dto.payment.PaymentCheckRequestDto;
import com.devops.paylite.service.dto.payment.PaymentCheckResponseDto;
import com.devops.paylite.service.dto.payment.PaymentCreateRequestDto;
import com.devops.paylite.service.dto.PaymentDTO;
import com.devops.paylite.service.dto.RecipientCatalogDto;
import com.devops.paylite.service.dto.RecipientFieldCatalogDto;
import com.devops.paylite.service.dto.RecipientFieldValueCatalogDto;
import com.devops.paylite.mapper.PaymentCheckMapper;
import com.devops.paylite.mapper.PaymentMapper;
import com.devops.paylite.mapper.PaymentPayMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.devops.paylite.web.rest.PaynetServiceClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentCheckMapper paymentCheckMapper;
    private final PaymentPayMapper paymentPayMapper;
    private final PaymentProperties paymentProperties;
    private final PaynetServiceClient paynetServiceClient;
    private final PaynetRecipientRepository paynetRecipientRepository;
    private final PaynetRecipientFieldRepository paynetRecipientFieldRepository;
    private final PaymentTransactionService paymentTransactionService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<RecipientCatalogDto> getRecipients(Pageable pageable) {
        return paynetRecipientRepository
            .findAllByActiveTrueOrderByNameAsc(pageable)
            .map(this::toRecipientCatalogDto);
    }


    @Transactional(readOnly = true)
    public List<RecipientFieldCatalogDto> getRecipientFields(Long recipientId, RecipientFieldMethod method) {
        return paynetRecipientFieldRepository
            .findAllByRecipientPaynetIdAndActiveTrueAndMethodOrderBySortOrderAscIdAsc(recipientId, method)
            .stream()
            .map(this::toRecipientFieldCatalogDto)
            .toList();
    }

    /**
     * Save a payment.
     *
     * @param dto the entity to create.
     * @return the persisted entity.
     */
    public PaymentDTO create(PaymentCreateRequestDto dto) {
        LOG.debug("Request to create Payment : {}", dto);

        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current user login not found"));

        validateAmount(dto.getAmount());

        long commissionAmount = calculateCommission(dto.getAmount());
        long totalAmount = calculateTotalAmount(dto.getAmount(), commissionAmount);
        String requestPayload = serializePaynetRequest(paymentPayMapper.toPaynetPayRequest(dto));

        Payment payment;
        try {
            payment = paymentTransactionService.prepare(dto, login, commissionAmount, totalAmount, requestPayload);
        } catch (DataIntegrityViolationException exception) {
            payment = paymentTransactionService.findReplay(dto.getIdempotencyKey(), dto, login);
        }

        if (payment.getStatus() != PaymentStatus.PENDING || payment.getAttemptCount() > 0) {
            return paymentMapper.toDto(payment);
        }
        return paymentMapper.toDto(processPendingPayment(payment.getId()));
    }

    public void reconcilePendingPayments() {
        Instant updatedBefore = Instant.now().minusSeconds(60);
        paymentRepository
            .findTop50ByStatusAndUpdatedDateBeforeOrderByUpdatedDateAsc(PaymentStatus.PENDING, updatedBefore)
            .forEach(payment -> processPendingPayment(payment.getId()));
    }

    private Payment processPendingPayment(Long paymentId) {
        Payment payment = paymentTransactionService.markAttempt(paymentId);
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return payment;
        }

        final com.devops.paylite.service.dto.paynet.PaynetPayRequestDto request;
        try {
            request = objectMapper.readValue(
                payment.getRequestPayload(),
                com.devops.paylite.service.dto.paynet.PaynetPayRequestDto.class
            );
        } catch (JsonProcessingException exception) {
            return paymentTransactionService.markFailedAndReleaseBalance(paymentId, "Invalid stored payment payload");
        }

        try {
            PaynetPayResponseDto response = paynetServiceClient.pay(payment.getRecipientId(), request);
            if (response.isPaid()) {
                return paymentTransactionService.markPaid(paymentId, response.getPaymentId());
            }

            List<String> errors = response.getErrors() == null ? List.of() : response.getErrors();
            String message = errors.isEmpty() ? "Paynet payment failed" : "Paynet payment failed: " + String.join(", ", errors);
            return paymentTransactionService.markFailedAndReleaseBalance(paymentId, message);
        } catch (RuntimeException exception) {
            LOG.warn("Paynet payment attempt failed; payment remains PENDING id={}", paymentId, exception);
            return paymentTransactionService.markPendingError(paymentId, exception.getMessage());
        }
    }

    private String serializePaynetRequest(com.devops.paylite.service.dto.paynet.PaynetPayRequestDto request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize payment request", exception);
        }
    }

    public Page<PaymentDTO> getOwn(Pageable pageable) {
        LOG.debug("Request to get all Own Payments");

        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current user login not found"));

        return paymentRepository.findAllByAgent_Login(login, pageable).map(paymentMapper::toDto);
    }

    private long calculateCommission(long amount) {
        BigDecimal commissionPercent = validateCommissionPercent();

        try {
            return BigDecimal.valueOf(amount)
                .multiply(commissionPercent)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .longValueExact();
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount is too large", exception);
        }
    }

    private BigDecimal validateCommissionPercent() {
        BigDecimal commissionPercent = paymentProperties.getCommissionPercent();

        if (
            commissionPercent == null ||
            commissionPercent.compareTo(BigDecimal.ZERO) < 0 ||
            commissionPercent.compareTo(BigDecimal.valueOf(100)) > 0
        ) {
            throw new IllegalStateException("Commission percent must be between 0 and 100");
        }

        return commissionPercent;
    }

    private long calculateTotalAmount(long amount, long commissionAmount) {
        try {
            return Math.addExact(amount, commissionAmount);
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount is too large", exception);
        }
    }

    public PaymentCheckResponseDto check(PaymentCheckRequestDto createDto) {
        PaynetCheckResponseDto check = paynetServiceClient
            .check(createDto.getRecipientId(), paymentCheckMapper.toPaynetCheckRequest(createDto));

        return paymentCheckMapper.toPaymentCheckResponse(check);
    }

    private void validateAmount(long amount) {
        if (amount < paymentProperties.getMinAmount()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Payment amount is below the minimum limit"
            );
        }

        if (amount > paymentProperties.getMaxAmount()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Payment amount exceeds the maximum limit"
            );
        }
    }


    private RecipientFieldCatalogDto toRecipientFieldCatalogDto(PaynetRecipientField field) {
        PaynetRecipient recipient = field.getRecipient();
        List<RecipientFieldValueCatalogDto> values = field
            .getValues()
            .stream()
            .filter(value -> Boolean.TRUE.equals(value.getActive()))
            .map(this::toRecipientFieldValueCatalogDto)
            .toList();

        return new RecipientFieldCatalogDto(
            field.getPaynetId(),
            field.getName(),
            field.getLabel(),
            field.getValidationRegex(),
            field.getMethod().name(),
            field.getActive(),
            field.getRequired(),
            field.getType().name(),
            field.getSortOrder(),
            recipient.getPaynetId(),
            recipient.getName(),
            recipient.getCategory().getName(),
            values
        );
    }


    private RecipientCatalogDto toRecipientCatalogDto(PaynetRecipient recipient) {
        return new RecipientCatalogDto(
            recipient.getPaynetId(),
            recipient.getName(),
            recipient.getCode(),
            recipient.getActive(),
            recipient.getCategory().getName(),
            recipient.getCategory().getPaynetId()
        );
    }


    private RecipientFieldValueCatalogDto toRecipientFieldValueCatalogDto(PaynetRecipientFieldValue value) {
        return new RecipientFieldValueCatalogDto(
            value.getPaynetId(),
            value.getValue(),
            value.getLabel(),
            value.getActive(),
            value.getSortOrder(),
            value.getRecipientField().getPaynetId(),
            value.getParent() == null ? null : value.getParent().getPaynetId()
        );
    }
}
