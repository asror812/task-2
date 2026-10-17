package com.devops.paynet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devops.paynet.domain.Category;
import com.devops.paynet.config.PaymentProperties;
import com.devops.paynet.domain.Recipient;
import com.devops.paynet.domain.RecipientField;
import com.devops.paynet.domain.RecipientFieldValue;
import com.devops.paynet.domain.enumeration.RecipientFieldType;
import com.devops.paynet.repository.RecipientFieldValueRepository;
import com.devops.paynet.repository.RecipientRepository;
import com.devops.paynet.repository.CategoryRepository;
import com.devops.paynet.dto.CheckRequestDto;
import com.devops.paynet.dto.CheckResponseDto;
import com.devops.paynet.dto.PayRequestDto;
import com.devops.paynet.dto.PayResponseDto;
import com.devops.paynet.dto.RecipientCreateDto;
import com.devops.paynet.dto.RecipientResponseDto;
import com.devops.paynet.mapper.RecipientMapper;
import com.devops.paynet.mapper.RecipientFieldMapper;
import com.devops.paynet.web.rest.errors.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RecipientServiceTest {

    @Mock
    private RecipientRepository recipientRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private final RecipientMapper recipientMapper = new RecipientMapper() {};
    private final RecipientFieldMapper recipientFieldMapper = new RecipientFieldMapper() {};

    @Mock
    private RecipientFieldValueRepository recipientFieldValueRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private PaymentProperties paymentProperties;

    private RecipientService service;
    private Recipient recipient;
    private RecipientField regionField;
    private RecipientField districtField;
    private RecipientFieldValue tashkent;
    private RecipientFieldValue yunusabad;

    @BeforeEach
    void setUp() {
        paymentProperties = new PaymentProperties();
        service = new RecipientService(
            recipientRepository,
            categoryRepository,
            recipientMapper,
            recipientFieldMapper,
            recipientFieldValueRepository,
            redisTemplate,
            new ObjectMapper(),
            paymentProperties
        );

        recipient = new Recipient();
        recipient.setId(3L);
        recipient.setCode("GAS");
        recipient.setName("Gas");
        recipient.setActive(true);

        regionField = selectField(7L, "regionId");
        districtField = selectField(8L, "districtId");
        recipient.setRecipientFields(List.of(regionField, districtField));

        tashkent = option(101L, "1", regionField, null);
        yunusabad = option(201L, "10", districtField, tashkent);

        lenient().when(recipientRepository.findOneWithRecipientFields(3L)).thenReturn(Optional.of(recipient));
    }

    @Test
    void createsRecipientWithCategoryLoadedFromDatabase() {
        RecipientCreateDto request = new RecipientCreateDto("Beeline", "BEELINE", true, 5L);
        Category category = new Category();
        category.setId(5L);
        category.setName("Mobile operators");
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(recipientRepository.save(any(Recipient.class))).thenAnswer(invocation -> {
            Recipient saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        RecipientResponseDto response = service.save(request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.categoryId()).isEqualTo(5L);
        assertThat(response.categoryName()).isEqualTo("Mobile operators");
    }

    @Test
    void rejectsRecipientWhenCategoryDoesNotExist() {
        RecipientCreateDto request = new RecipientCreateDto("Beeline", "BEELINE", true, 999L);
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(request))
            .isInstanceOf(ResourceNotFoundException.class);
        verify(recipientRepository, never()).save(any(Recipient.class));
    }

    @Test
    void acceptsActiveChildOptionBelongingToSelectedParent() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(recipientFieldValueRepository.findOneByRecipientField_IdAndValueAndActiveTrue(7L, "1"))
            .thenReturn(Optional.of(tashkent));
        when(recipientFieldValueRepository.findOneByRecipientField_IdAndValueAndActiveTrue(8L, "10"))
            .thenReturn(Optional.of(yunusabad));

        CheckResponseDto response = service.checkRecipient(
            3L,
            new CheckRequestDto(Map.of("regionId", "1", "districtId", "10"), 10_000L)
        );

        assertThat(response.isValid()).isTrue();
        assertThat(response.getCheckId()).isNotBlank();
        assertThat(response.getPaymentReference()).isEqualTo("regionId: 1, districtId: 10");
        assertThat(response.getErrors()).isEmpty();
        verify(valueOperations).set(any(String.class), any(String.class), eq(Duration.ofMinutes(15)));
    }

    @Test
    void rejectsChildOptionWhenItsParentWasNotSelected() {
        when(recipientFieldValueRepository.findOneByRecipientField_IdAndValueAndActiveTrue(7L, "2"))
            .thenReturn(Optional.of(option(102L, "2", regionField, null)));
        when(recipientFieldValueRepository.findOneByRecipientField_IdAndValueAndActiveTrue(8L, "10"))
            .thenReturn(Optional.of(yunusabad));

        CheckResponseDto response = service.checkRecipient(
            3L,
            new CheckRequestDto(Map.of("regionId", "2", "districtId", "10"), 10_000L)
        );

        assertThat(response.isValid()).isFalse();
        assertThat(response.getErrors()).contains("districtId does not belong to selected regionId");
    }

    @Test
    void rejectsUnknownSelectOption() {
        when(recipientFieldValueRepository.findOneByRecipientField_IdAndValueAndActiveTrue(eq(7L), any()))
            .thenReturn(Optional.empty());
        when(recipientFieldValueRepository.findOneByRecipientField_IdAndValueAndActiveTrue(eq(8L), any()))
            .thenReturn(Optional.empty());

        CheckResponseDto response = service.checkRecipient(
            3L,
            new CheckRequestDto(Map.of("regionId", "999", "districtId", "999"), 10_000L)
        );

        assertThat(response.isValid()).isFalse();
        assertThat(response.getErrors())
            .contains("regionId contains an unsupported value", "districtId contains an unsupported value");
    }

    @Test
    void rejectsFieldsThatAreNotConfiguredForCheck() {
        when(recipientFieldValueRepository.findOneByRecipientField_IdAndValueAndActiveTrue(7L, "1"))
            .thenReturn(Optional.of(tashkent));
        when(recipientFieldValueRepository.findOneByRecipientField_IdAndValueAndActiveTrue(8L, "10"))
            .thenReturn(Optional.of(yunusabad));

        CheckResponseDto response = service.checkRecipient(
            3L,
            new CheckRequestDto(Map.of("regionId", "1", "districtId", "10", "unexpected", "secret"), 10_000L)
        );

        assertThat(response.isValid()).isFalse();
        assertThat(response.getCheckId()).isNull();
        assertThat(response.getErrors()).containsExactly("unexpected is not a supported CHECK field");
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void paysWhenCheckExists() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("paynet:pay:lock:request-1"), any(String.class), any())).thenReturn(true);
        when(valueOperations.get("paynet:pay:idempotency:request-1")).thenReturn(null);
        when(valueOperations.get("paynet:check:check-1")).thenReturn(
            new ObjectMapper().writeValueAsString(Map.of(
                "checkId", "check-1",
                "recipientId", 3L,
                "recipientName", "Gas",
                "amount", 10_000L,
                "paymentReference", "regionId: 1, districtId: 10",
                "fields", Map.of("regionId", "1", "districtId", "10")
            ))
        );

        PayResponseDto response = service.payRecipient(
            3L,
            new PayRequestDto("check-1", "request-1")
        );

        assertThat(response.isPaid()).isTrue();
        assertThat(response.getPaymentId()).isNotBlank();
        assertThat(response.getCheckId()).isEqualTo("check-1");
        verify(redisTemplate).delete("paynet:check:check-1");
        verify(valueOperations).setIfAbsent(eq("paynet:pay:lock:request-1"), any(String.class), eq(Duration.ofSeconds(60)));
        verify(valueOperations).set(eq("paynet:pay:idempotency:request-1"), any(String.class), eq(Duration.ofDays(7)));
    }

    @Test
    void rejectsInvalidDateWithoutCustomRegex() {
        RecipientField dateField = new RecipientField();
        dateField.setId(20L);
        dateField.setName("billingDate");
        dateField.setLabel("Billing date");
        dateField.setType(RecipientFieldType.DATE);
        dateField.setRequired(true);
        dateField.setActive(true);
        dateField.setSortOrder(1);
        dateField.setRecipient(recipient);
        recipient.setRecipientFields(List.of(dateField));

        CheckResponseDto response = service.checkRecipient(
            3L,
            new CheckRequestDto(Map.of("billingDate", "not-a-date"), 10_000L)
        );

        assertThat(response.isValid()).isFalse();
        assertThat(response.getErrors()).containsExactly("billingDate has invalid DATE format");
    }

    @Test
    void rejectsRecipientWithoutActiveFields() {
        recipient.setRecipientFields(List.of());

        CheckResponseDto response = service.checkRecipient(
            3L,
            new CheckRequestDto(Map.of("account", "123"), 10_000L)
        );

        assertThat(response.isValid()).isFalse();
        assertThat(response.getErrors()).containsExactly("Recipient has no active fields");
    }

    private RecipientField selectField(Long id, String name) {
        RecipientField field = new RecipientField();
        field.setId(id);
        field.setName(name);
        field.setLabel(name);
        field.setType(RecipientFieldType.SELECT);
        field.setRequired(true);
        field.setActive(true);
        field.setSortOrder(id.intValue());
        field.setRecipient(recipient);
        return field;
    }

    private RecipientFieldValue option(
        Long id,
        String value,
        RecipientField field,
        RecipientFieldValue parent
    ) {
        RecipientFieldValue option = new RecipientFieldValue();
        option.setId(id);
        option.setValue(value);
        option.setLabel(value);
        option.setActive(true);
        option.setSortOrder(id.intValue());
        option.setRecipientField(field);
        option.setParent(parent);
        return option;
    }
}
