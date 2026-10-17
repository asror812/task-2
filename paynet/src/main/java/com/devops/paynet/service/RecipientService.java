package com.devops.paynet.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.devops.paynet.domain.RecipientField;
import com.devops.paynet.domain.RecipientFieldValue;
import com.devops.paynet.domain.enumeration.RecipientFieldType;
import com.devops.paynet.domain.Recipient;
import com.devops.paynet.config.PaymentProperties;
import com.devops.paynet.repository.RecipientRepository;
import com.devops.paynet.repository.CategoryRepository;
import com.devops.paynet.repository.RecipientFieldValueRepository;
import com.devops.paynet.dto.CheckRequestDto;
import com.devops.paynet.dto.CheckDetailsDto;
import com.devops.paynet.dto.CheckResponseDto;
import com.devops.paynet.dto.CategoryResponseDto;
import com.devops.paynet.dto.PayRequestDto;
import com.devops.paynet.dto.PayResponseDto;
import com.devops.paynet.dto.PaymentLookupStatus;
import com.devops.paynet.dto.PaymentStatusResponseDto;
import com.devops.paynet.dto.RecipientCreateDto;
import com.devops.paynet.dto.RecipientCatalogResponseDto;
import com.devops.paynet.dto.RecipientFieldResponseDto;
import com.devops.paynet.dto.RecipientResponseDto;
import com.devops.paynet.mapper.RecipientFieldMapper;
import com.devops.paynet.mapper.RecipientMapper;
import com.devops.paynet.web.rest.errors.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RecipientService {

    private static final Logger LOG = LoggerFactory.getLogger(RecipientService.class);

    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
        "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
        Long.class
    );

    private final RecipientRepository recipientRepository;
    private final CategoryRepository categoryRepository;
    private final RecipientMapper recipientMapper;
    private final RecipientFieldMapper recipientFieldMapper;
    private final RecipientFieldValueRepository recipientFieldValueRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final PaymentProperties paymentProperties;

    /**
     * Save a recipient.
     *
     * @param recipientDTO the entity to save.
     * @return the persisted entity.
     */
    @Transactional
    public RecipientResponseDto save(RecipientCreateDto recipientDTO) {
        LOG.debug("Request to save Recipient : {}", recipientDTO);

        var category = categoryRepository.findById(recipientDTO.categoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category", recipientDTO.categoryId()));

        Recipient recipient = recipientMapper.toEntity(recipientDTO);
        recipient.setCategory(category);
        recipient = recipientRepository.save(recipient);
        return recipientMapper.toResponseDto(recipient);
    }

    /**
     * Get all the recipients.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<RecipientResponseDto> findAll(Pageable pageable, Instant updatedSince) {
        LOG.debug("Request to get all Recipients");
        Page<Recipient> recipients = updatedSince == null
            ? recipientRepository.findAllWithEagerRelationships(pageable)
            : recipientRepository.findAllUpdatedSince(updatedSince, pageable);
        return recipients.map(recipientMapper::toResponseDto);
    }

    /**
     * Get one recipient by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    public RecipientResponseDto findOne(Long id) {
        LOG.debug("Request to get Recipient : {}", id);
        return recipientRepository
                .findOneWithEagerRelationships(id)
                .map(recipientMapper::toResponseDto)
                .orElseThrow(() -> new ResourceNotFoundException("Recipient", id));
    }

    @Transactional(readOnly = true)
    public List<RecipientCatalogResponseDto> findCatalog(Instant updatedSince) {
        List<Recipient> recipients = updatedSince == null
                ? recipientRepository.findCatalog()
                : recipientRepository.findCatalogUpdatedSince(updatedSince);

        return recipients.stream()
            .map(recipient -> {

                List<RecipientFieldResponseDto> fields = recipient.getRecipientFields() == null
                    ? List.of()
                    : recipient.getRecipientFields().stream()
                        .sorted(Comparator.comparingInt(RecipientField::getSortOrder))
                        .collect(java.util.stream.Collectors.toMap(
                            RecipientField::getId,
                            field -> field,
                            (first, ignored) -> first,
                            LinkedHashMap::new
                        ))
                        .values()
                        .stream()
                        .map(recipientFieldMapper::toResponseDto)
                        .toList();

                var category = recipient.getCategory();
                CategoryResponseDto categoryDto = new CategoryResponseDto(
                    category.getId(),
                    category.getName(),
                    category.getActive(),
                    category.getSortOrder(),
                    category.getLastModifiedDate()
                );

                return new RecipientCatalogResponseDto(categoryDto, recipientMapper.toResponseDto(recipient), fields);
            })
            .toList();
    }

    @Transactional
    public RecipientResponseDto updateActive(Long id, boolean active) {
        Recipient recipient = recipientRepository.findOneWithEagerRelationships(id)
            .orElseThrow(() -> new ResourceNotFoundException("Recipient", id));
        recipient.setActive(active);
        return recipientMapper.toResponseDto(recipientRepository.save(recipient));
    }


    @Transactional
    public CheckResponseDto checkRecipient(Long recipientId, CheckRequestDto checkRequestDto) {
        LOG.debug("Request to check Recipient : {}", recipientId);

        Recipient recipient = recipientRepository.findOneWithRecipientFields(recipientId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Recipient", recipientId));

        if (!Boolean.TRUE.equals(recipient.getActive())) {
            return new CheckResponseDto(
                    false,
                    recipient.getName(),
                    recipient.getId(),
                    List.of("Recipient is not active"));
        }

        if (!hasActiveFields(recipient)) {
            return new CheckResponseDto(false, recipient.getName(), recipient.getId(), List.of("Recipient has no active fields"));
        }

        List<String> validationErrors = validateCheckFields(recipient, checkRequestDto);
        if (!validationErrors.isEmpty()) {
            return new CheckResponseDto(
                    false,
                    recipient.getName(),
                    recipient.getId(),
                    validationErrors);
        }

        CheckResponseDto checkResponseDto = new CheckResponseDto(
                true,
                recipient.getName(),
                recipient.getId(),
                List.of());

        if (checkResponseDto.isValid()) {
            String checkId = UUID.randomUUID().toString();
            String paymentReference = buildPaymentReference(recipient, checkRequestDto.getFields());
            checkResponseDto.setCheckId(checkId);
            checkResponseDto.setPaymentReference(paymentReference);
            redisTemplate.opsForValue().set(redisKey(checkId), toJson(checkResponseDto, checkRequestDto), paymentProperties.getCheckTtl());
        }

        return checkResponseDto;
    }

    public CheckDetailsDto getCheckDetails(String checkId) {
        String storedCheck = redisTemplate.opsForValue().get(redisKey(checkId));
        if (storedCheck == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Check ID is expired or not found");
        }

        StoredCheckData checkData = readStoredCheck(storedCheck);
        return new CheckDetailsDto(
                checkData.checkId(),
                checkData.recipientId(),
                checkData.amount(),
                checkData.paymentReference()
        );
    }

    public PayResponseDto payRecipient(Long recipientId, PayRequestDto payRequestDto) {
        LOG.info("Payment processing started recipientId={}", recipientId);

        PayResponseDto existingResponse = findExistingPayResponse(recipientId, payRequestDto);

        if (existingResponse != null) {
            LOG.info("Payment response replayed recipientId={} paymentId={}", recipientId, existingResponse.getPaymentId());
            return existingResponse;
        }

        String lockKey = payIdempotencyLockKey(payRequestDto.getIdempotencyKey());
        String lockToken = UUID.randomUUID().toString();
        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockToken, paymentProperties.getProcessingLockTtl());

        if (!Boolean.TRUE.equals(lockAcquired)) {
            PayResponseDto completedResponse = findExistingPayResponse(recipientId, payRequestDto);
            if (completedResponse != null) {
                return completedResponse;
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment with this idempotency key is already processing");
        }

        try {
            return processPayment(recipientId, payRequestDto);
        } finally {
            redisTemplate.execute(RELEASE_LOCK_SCRIPT, List.of(lockKey), lockToken);
        }
    }

    private PayResponseDto processPayment(Long recipientId, PayRequestDto payRequestDto) {
        String storedCheck = redisTemplate.opsForValue().get(redisKey(payRequestDto.getCheckId()));
        if (storedCheck == null) {
            LOG.warn("Payment rejected recipientId={} reason=CHECK_NOT_FOUND", recipientId);
            return new PayResponseDto(
                    false,
                    null,
                    recipientId,
                    null,
                    List.of("Check ID is expired or not found"));
        }

        StoredCheckData checkData = readStoredCheck(storedCheck);
        if (!recipientId.equals(checkData.recipientId())) {
            return new PayResponseDto(
                    false,
                    null,
                    recipientId,
                    checkData.recipientName(),
                    List.of("Check ID belongs to another recipient"));
        }

        String paymentId = UUID.randomUUID().toString();

        PayResponseDto response = new PayResponseDto(
            true,
            paymentId,
            checkData.recipientId(),
            checkData.recipientName(),
            List.of()
        );

        response.setCheckId(payRequestDto.getCheckId());
        redisTemplate
            .opsForValue()
            .set(payIdempotencyKey(payRequestDto.getIdempotencyKey()), toJson(response), paymentProperties.getIdempotencyTtl());
        redisTemplate.delete(redisKey(payRequestDto.getCheckId()));

        LOG.info("Payment completed recipientId={} paymentId={}", recipientId, paymentId);
        return response;
    }

    private List<String> validateCheckFields(Recipient recipient, CheckRequestDto checkRequestDto) {
        List<String> errors = new ArrayList<>();
        Map<String, Object> requestFields = checkRequestDto == null ? null : checkRequestDto.getFields();

        if (checkRequestDto == null || checkRequestDto.getAmount() == null || checkRequestDto.getAmount() <= 0) {
            errors.add("amount must be positive");
        }

        if (requestFields == null) {
            errors.add("Fields cannot be empty");
            return errors;
        }

        if (recipient.getRecipientFields() == null) {
            return errors;
        }

        Set<String> allowedFieldNames = recipient.getRecipientFields().stream()
            .filter(field -> Boolean.TRUE.equals(field.getActive()))
            .map(RecipientField::getName)
            .collect(java.util.stream.Collectors.toSet());

        requestFields.keySet().stream()
            .filter(fieldName -> fieldName == null || !allowedFieldNames.contains(fieldName))
            .map(fieldName -> fieldName == null ? "<null>" : fieldName)
            .sorted()
            .forEach(fieldName -> errors.add(fieldName + " is not a supported CHECK field"));

        for (RecipientField field : recipient.getRecipientFields()) {
            if (!Boolean.TRUE.equals(field.getActive())) {
                continue;
            }

            Object value = requestFields.get(field.getName());
            String valueAsString = value == null ? null : value.toString().trim();

            if (Boolean.TRUE.equals(field.getRequired()) && (valueAsString == null || valueAsString.isBlank())) {
                errors.add(field.getName() + " is required");
                continue;
            }

            if (valueAsString != null && !valueAsString.isBlank() && !hasValidBaseFormat(field.getType(), valueAsString)) {
                errors.add(field.getName() + " has invalid " + field.getType() + " format");
                continue;
            }

            if (valueAsString != null && !valueAsString.isBlank() && field.getValidationRegex() != null && !field.getValidationRegex().isBlank()) {
                if (!valueAsString.matches(field.getValidationRegex())) {
                    errors.add(field.getName() + " is invalid");
                }
            }

            validateSelectValue(field, valueAsString, requestFields).ifPresent(errors::add);
        }

        return errors;
    }

    private Optional<String> validateSelectValue(RecipientField field, String value, Map<String, Object> requestFields) {
        if (field.getType() != RecipientFieldType.SELECT || value == null || value.isBlank()) {
            return Optional.empty();
        }

        Optional<RecipientFieldValue> selectedValue = recipientFieldValueRepository
            .findOneByRecipientField_IdAndValueAndActiveTrue(field.getId(), value);
        if (selectedValue.isEmpty()) {
            return Optional.of(field.getName() + " contains an unsupported value");
        }

        RecipientFieldValue parent = selectedValue.get().getParent();
        if (parent == null) {
            return Optional.empty();
        }

        String parentFieldName = parent.getRecipientField().getName();
        Object requestedParentValue = requestFields.get(parentFieldName);
        if (requestedParentValue == null || !parent.getValue().equals(requestedParentValue.toString().trim())) {
            return Optional.of(field.getName() + " does not belong to selected " + parentFieldName);
        }
        return Optional.empty();
    }

    private String buildPaymentReference(Recipient recipient, Map<String, Object> fields) {
        if (recipient.getRecipientFields() == null) {
            return recipient.getName();
        }

        String reference = recipient
                .getRecipientFields()
                .stream()
                .filter(field -> Boolean.TRUE.equals(field.getActive()))
                .sorted(
                        Comparator.comparing(
                                        RecipientField::getSortOrder,
                                        Comparator.nullsLast(Integer::compareTo)
                                )
                                .thenComparing(RecipientField::getId, Comparator.nullsLast(Long::compareTo))
                )
                .map(field -> formatReferencePart(field, fields.get(field.getName())))
                .filter(Objects::nonNull)
                .reduce((left, right) -> left + ", " + right)
                .orElse("");

        return reference.isBlank() ? recipient.getName() : reference;
    }

    private String formatReferencePart(RecipientField field, Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return field.getLabel() + ": " + value.toString().trim();
    }

    private boolean hasActiveFields(Recipient recipient) {
        return recipient.getRecipientFields() != null && recipient.getRecipientFields().stream()
                .anyMatch(field -> Boolean.TRUE.equals(field.getActive()));
    }

    private boolean hasValidBaseFormat(RecipientFieldType type, String value) {
        try {
            return switch (type) {
                case TEXT, SELECT -> true;
                case NUMBER -> {
                    new BigDecimal(value.replace(',', '.'));
                    yield value.matches("^[+-]?\\d+([.,]\\d+)?$");
                }
                case PHONE -> value.matches("^\\+?[0-9]{7,15}$");
                case DATE -> {
                    LocalDate.parse(value);
                    yield true;
                }
            };
        } catch (NumberFormatException | DateTimeParseException exception) {
            return false;
        }
    }

    private String redisKey(String checkId) {
        return "paynet:check:" + checkId;
    }

    private String payIdempotencyKey(String idempotencyKey) {
        return "paynet:pay:idempotency:" + idempotencyKey;
    }

    private String payIdempotencyLockKey(String idempotencyKey) {
        return "paynet:pay:lock:" + idempotencyKey;
    }

    public PaymentStatusResponseDto getPaymentStatus(Long recipientId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency key is required");
        }

        String storedResponse = redisTemplate.opsForValue().get(payIdempotencyKey(idempotencyKey));
        if (storedResponse != null) {
            try {
                PayResponseDto response = objectMapper.readValue(storedResponse, PayResponseDto.class);
                if (!recipientId.equals(response.getRecipientId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key belongs to another recipient");
                }
                return new PaymentStatusResponseDto(
                    response.isPaid() ? PaymentLookupStatus.PAID : PaymentLookupStatus.FAILED,
                    response.getPaymentId(),
                    response.getErrors() == null ? List.of() : response.getErrors()
                );
            } catch (JsonProcessingException exception) {
                throw new RuntimeException("Failed to deserialize pay response", exception);
            }
        }

        if (redisTemplate.hasKey(payIdempotencyLockKey(idempotencyKey))) {
            return new PaymentStatusResponseDto(PaymentLookupStatus.PROCESSING, null, List.of());
        }

        return new PaymentStatusResponseDto(PaymentLookupStatus.NOT_FOUND, null, List.of());
    }

    private PayResponseDto findExistingPayResponse(Long recipientId, PayRequestDto payRequestDto) {
        if (
                payRequestDto == null ||
                        payRequestDto.getIdempotencyKey() == null ||
                        payRequestDto.getIdempotencyKey().isBlank()) {
            return null;
        }

        String storedResponse = redisTemplate.opsForValue()
                .get(payIdempotencyKey(payRequestDto.getIdempotencyKey()));

        if (storedResponse == null) return null;

        try {
            PayResponseDto response = objectMapper.readValue(storedResponse, PayResponseDto.class);

            if (!recipientId.equals(response.getRecipientId())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Idempotency key belongs to another recipient"
                );
            }
            if (response.getCheckId() != null && !payRequestDto.getCheckId().equals(response.getCheckId())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Idempotency key belongs to another check"
                );
            }
            return response;
        } catch (JsonProcessingException exception) {
            throw new RuntimeException("Failed to deserialize pay response", exception);
        }
    }

    private String toJson(PayResponseDto payResponseDto) {
        try {
            return objectMapper.writeValueAsString(payResponseDto);
        } catch (JsonProcessingException exception) {
            throw new RuntimeException("Failed to serialize pay response", exception);
        }
    }

    private String toJson(CheckResponseDto checkResponseDto, CheckRequestDto checkRequestDto) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "checkId", checkResponseDto.getCheckId(),
                    "recipientId", checkResponseDto.getRecipientId(),
                    "recipientName", checkResponseDto.getRecipientName(),
                    "amount", checkRequestDto.getAmount(),
                    "paymentReference", checkResponseDto.getPaymentReference()
            ));
        } catch (JsonProcessingException exception) {
            throw new RuntimeException("Failed to serialize check data", exception);
        }
    }

    private StoredCheckData readStoredCheck(String json) {
        try {
            return objectMapper.readValue(json, StoredCheckData.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize check data", exception);
        }
    }

    private record StoredCheckData(
        String checkId,
        Long recipientId,
        String recipientName,
        Long amount,
        String paymentReference
    ) {}
}
