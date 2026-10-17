package com.devops.paylite.service;

import com.devops.paylite.config.PaymentProperties;
import com.devops.paylite.domain.Payment;
import com.devops.paylite.domain.PaynetRecipient;
import com.devops.paylite.domain.PaynetRecipientField;
import com.devops.paylite.domain.PaynetRecipientFieldValue;
import com.devops.paylite.domain.enumeration.PaymentStatus;
import com.devops.paylite.mapper.PaymentCheckMapper;
import com.devops.paylite.mapper.PaymentMapper;
import com.devops.paylite.repository.PaymentRepository;
import com.devops.paylite.repository.PaynetRecipientFieldRepository;
import com.devops.paylite.repository.PaynetRecipientRepository;
import com.devops.paylite.security.SecurityUtils;
import com.devops.paylite.service.dto.PaymentDTO;
import com.devops.paylite.service.dto.RecipientCatalogDto;
import com.devops.paylite.service.dto.RecipientFieldCatalogDto;
import com.devops.paylite.service.dto.RecipientFieldValueCatalogDto;
import com.devops.paylite.service.dto.payment.PaymentCheckRequestDto;
import com.devops.paylite.service.dto.payment.PaymentCheckResponseDto;
import com.devops.paylite.service.dto.payment.PaymentCreateRequestDto;
import com.devops.paylite.service.dto.paynet.PaynetCheckDetailsDto;
import com.devops.paylite.service.dto.paynet.PaynetCheckRequestDto;
import com.devops.paylite.service.dto.paynet.PaynetCheckResponseDto;
import com.devops.paylite.service.dto.paynet.PaynetPayRequestDto;
import com.devops.paylite.service.dto.paynet.PaynetPayResponseDto;
import com.devops.paylite.service.dto.paynet.PaynetPaymentStatusResponseDto;
import com.devops.paylite.web.rest.PaynetServiceClient;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentCheckMapper paymentCheckMapper;
    private final PaymentProperties paymentProperties;
    private final PaynetServiceClient paynetServiceClient;
    private final PaynetRecipientRepository paynetRecipientRepository;
    private final PaynetRecipientFieldRepository paynetRecipientFieldRepository;
    private final PaymentTransactionService paymentTransactionService;

    public Page<RecipientCatalogDto> getRecipients(Pageable pageable) {
        return paynetRecipientRepository.findAllByActiveTrueOrderByNameAsc(pageable).map(this::toRecipientCatalogDto);
    }

    public List<RecipientFieldCatalogDto> getRecipientFields(Long recipientId) {
        return paynetRecipientFieldRepository
            .findAllByRecipientPaynetIdAndActiveTrueOrderBySortOrderAscIdAsc(recipientId)
            .stream()
            .map(this::toRecipientFieldCatalogDto)
            .toList();
    }

    public PaymentDTO create(PaymentCreateRequestDto dto) {
        LOG.debug("Request to create Payment : {}", dto);

        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current user login not found"));

        Optional<Payment> replay = paymentTransactionService.findReplayIfPresent(dto, login);

        if (replay.isPresent()) {
            return paymentMapper.toDto(replay.get());
        }

        PaynetCheckDetailsDto checkDetails = paynetServiceClient.getCheckDetails(dto.getCheckId());

        validateCheckDetails(dto, checkDetails);
        validateAmount(checkDetails.amount());

        long commissionAmount = calculateCommission(checkDetails.amount());
        long totalAmount = calculateTotalAmount(checkDetails.amount(), commissionAmount);

        Payment payment;

        try {
            payment = paymentTransactionService.prepare(dto, checkDetails, login, commissionAmount, totalAmount);
        } catch (DataIntegrityViolationException exception) {
            payment = paymentTransactionService.resolveCreationConstraintViolation(dto, login, exception);
        }

        return paymentMapper.toDto(payment);
    }

    private void validateCheckDetails(PaymentCreateRequestDto dto, PaynetCheckDetailsDto checkDetails) {
        if (
            checkDetails == null ||
            !dto.getCheckId().equals(checkDetails.checkId()) ||
            checkDetails.recipientId() == null ||
            checkDetails.amount() == null ||
            checkDetails.paymentReference() == null ||
            checkDetails.paymentReference().isBlank()
        ) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Paynet returned invalid check details");
        }
    }

    public Payment processPendingPayment(Long paymentId) {
        Optional<Payment> claimedPayment = paymentTransactionService.claimForProcessing(paymentId);

        if (claimedPayment.isEmpty()) {
            return paymentTransactionService.findWithAgent(paymentId);
        }

        Payment payment = claimedPayment.get();

        PaynetPayRequestDto request = new PaynetPayRequestDto(payment.getCheckId(), payment.getIdempotencyKey());

        try {
            if (payment.getAttemptCount() > 1) {
                PaynetPaymentStatusResponseDto statusResponse = paynetServiceClient.getPaymentStatus(
                    payment.getRecipientId(),
                    payment.getIdempotencyKey()
                );

                switch (statusResponse.status()) {
                    case PAID:
                        return paymentTransactionService.markPaid(paymentId, statusResponse.paymentId());
                    case PROCESSING:
                        return paymentTransactionService.markPendingError(paymentId, "Paynet payment is still processing");
                    case FAILED:
                        return paymentTransactionService.markFailedAndReleaseBalance(
                            paymentId,
                            paymentFailureMessage(statusResponse.errors())
                        );
                    case NOT_FOUND:
                        break;
                }
            }

            PaynetPayResponseDto response = paynetServiceClient.pay(payment.getRecipientId(), request);
            if (response.isPaid()) {
                return paymentTransactionService.markPaid(paymentId, response.getPaymentId());
            }

            return paymentTransactionService.markFailedAndReleaseBalance(paymentId, paymentFailureMessage(response.getErrors()));
        } catch (RuntimeException exception) {
            LOG.warn("Paynet payment attempt failed; payment remains PENDING id={}", paymentId, exception);
            return paymentTransactionService.markPendingError(paymentId, exception.getMessage());
        }
    }

    private String paymentFailureMessage(List<String> errors) {
        List<String> safeErrors = errors == null ? List.of() : errors;
        return safeErrors.isEmpty() ? "Paynet payment failed" : "Paynet payment failed: " + String.join(", ", safeErrors);
    }

    public Page<PaymentDTO> getOwn(Pageable pageable) {
        LOG.debug("Request to get all Own Payments");

        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current user login not found"));

        return paymentRepository.findAllByAgent_Login(login, pageable).map(paymentMapper::toDto);
    }

    public PaymentDTO getOwn(Long paymentId) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current user login not found"));

        Payment payment = paymentRepository
            .findOneWithAgentById(paymentId)
            .filter(candidate -> candidate.getAgent() != null && login.equals(candidate.getAgent().getLogin()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        return paymentMapper.toDto(payment);
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
        PaynetCheckRequestDto paynetCheckRequest = paymentCheckMapper.toPaynetCheckRequest(createDto);

        PaynetCheckResponseDto check = paynetServiceClient.check(createDto.getRecipientId(), paynetCheckRequest);

        return paymentCheckMapper.toPaymentCheckResponse(check);
    }

    private void validateAmount(long amount) {
        if (amount < paymentProperties.getMinAmount()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount is below the minimum limit");
        }

        if (amount > paymentProperties.getMaxAmount()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount exceeds the maximum limit");
        }
    }

    private RecipientFieldCatalogDto toRecipientFieldCatalogDto(PaynetRecipientField field) {
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
            field.getRequired(),
            field.getType().name(),
            values
        );
    }

    private RecipientCatalogDto toRecipientCatalogDto(PaynetRecipient recipient) {
        return new RecipientCatalogDto(
            recipient.getPaynetId(),
            recipient.getName(),
            recipient.getCategory().getName()
        );
    }

    private RecipientFieldValueCatalogDto toRecipientFieldValueCatalogDto(PaynetRecipientFieldValue value) {
        return new RecipientFieldValueCatalogDto(
            value.getPaynetId(),
            value.getValue(),
            value.getLabel(),
            value.getParent() == null ? null : value.getParent().getPaynetId()
        );
    }
}
