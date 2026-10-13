package com.devops.paylite.service;

import com.devops.paylite.config.PaymentProperties;
import com.devops.paylite.domain.Agent;
import com.devops.paylite.domain.Payment;
import com.devops.paylite.domain.PaynetRecipient;
import com.devops.paylite.domain.PaynetRecipientField;
import com.devops.paylite.domain.PaynetRecipientFieldValue;
import com.devops.paylite.domain.enumeration.PaymentStatus;
import com.devops.paylite.domain.enumeration.RecipientFieldMethod;
import com.devops.paylite.repository.AgentRepository;
import com.devops.paylite.repository.PaymentRepository;
import com.devops.paylite.repository.PaynetRecipientRepository;
import com.devops.paylite.repository.PaynetRecipientFieldRepository;
import com.devops.paylite.security.SecurityUtils;
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

import com.devops.paylite.web.rest.PaynetServiceClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final AgentRepository agentRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentCheckMapper paymentCheckMapper;
    private final PaymentPayMapper paymentPayMapper;
    private final PaymentProperties paymentProperties;
    private final PaynetServiceClient paynetServiceClient;
    private final PaynetRecipientRepository paynetRecipientRepository;
    private final PaynetRecipientFieldRepository paynetRecipientFieldRepository;

    @Transactional(readOnly = true)
    public List<RecipientCatalogDto> getRecipients() {
        return paynetRecipientRepository
            .findAllByActiveTrueOrderByNameAsc()
            .stream()
            .map(this::toRecipientCatalogDto)
            .toList();
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

    @Transactional(readOnly = true)
    public List<RecipientFieldCatalogDto> getRecipientFields(Long recipientId) {
        return paynetRecipientFieldRepository
            .findAllByRecipientPaynetIdAndActiveTrueAndMethodOrderBySortOrderAscIdAsc(
                recipientId,
                RecipientFieldMethod.CHECK
            )
            .stream()
            .map(this::toRecipientFieldCatalogDto)
            .toList();
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

    /**
     * Save a payment.
     *
     * @param dto the entity to create.
     * @return the persisted entity.
     */
    @Transactional
    public PaymentDTO create(PaymentCreateRequestDto dto) {
        LOG.debug("Request to create Payment : {}", dto);

        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current user login not found"));

        Agent agent = agentRepository
            .findByLoginForUpdate(login)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found"));

        Payment existingPayment = paymentRepository.findByIdempotencyKey(dto.getIdempotencyKey()).orElse(null);
        if (existingPayment != null) {
            if (existingPayment.getAgent() == null || !login.equals(existingPayment.getAgent().getLogin())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key already used");
            }
            if (
                !dto.getAmount().equals(existingPayment.getAmount()) ||
                !dto.getAccount().equals(existingPayment.getAccount()) ||
                !dto.getCheckId().equals(existingPayment.getCheckId())
            ) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key already used for another payment");
            }

            return paymentMapper.toDto(existingPayment);
        }

        validateAmount(dto.getAmount());

        long commissionAmount = calculateCommission(dto.getAmount());
        long totalAmount = calculateTotalAmount(dto.getAmount(), commissionAmount);

        PaymentStatus status = agent.getBalance() >= totalAmount ? PaymentStatus.PAID : PaymentStatus.FAILED;
        PaynetPayResponseDto paynetPayResponseDto = null;

        if (status == PaymentStatus.PAID) {
            paynetPayResponseDto = paynetServiceClient.pay(
                dto.getRecipientId(),
                paymentPayMapper.toPaynetPayRequest(dto)
            );

            if (!paynetPayResponseDto.isPaid()) {
                List<String> errors = paynetPayResponseDto.getErrors() == null ? List.of() : paynetPayResponseDto.getErrors();
                String message = errors.isEmpty() ? "Paynet payment failed" : "Paynet payment failed: " + String.join(", ", errors);

                throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    message
                );
            }
        }

        Payment payment = Payment.builder()
            .account(dto.getAccount())
            .amount(dto.getAmount())
            .totalAmount(totalAmount)
            .commissionAmount(commissionAmount)
            .checkId(dto.getCheckId())
            .paynetPaymentId(paynetPayResponseDto == null ? null : paynetPayResponseDto.getPaymentId())
            .idempotencyKey(dto.getIdempotencyKey())
            .status(status)
            .agent(agent)
            .createdDate(Instant.now())
            .build();

        if (status == PaymentStatus.PAID) {
            agent.setBalance(agent.getBalance() - totalAmount);
            agentRepository.save(agent);
        } else {
            LOG.info("Insufficient balance for agent: {}", agent.getLogin());
        }

        payment = paymentRepository.save(payment);
        return paymentMapper.toDto(payment);
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
        return paymentCheckMapper.toPaymentCheckResponse(
            paynetServiceClient.check(createDto.getRecipientId(), paymentCheckMapper.toPaynetCheckRequest(createDto))
        );
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
}
