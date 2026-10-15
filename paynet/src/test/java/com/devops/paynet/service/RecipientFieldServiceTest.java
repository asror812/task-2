package com.devops.paynet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devops.paynet.domain.Recipient;
import com.devops.paynet.domain.RecipientField;
import com.devops.paynet.domain.enumeration.RecipientFieldMethod;
import com.devops.paynet.domain.enumeration.RecipientFieldType;
import com.devops.paynet.dto.RecipientFieldCreateDto;
import com.devops.paynet.mapper.RecipientFieldMapper;
import com.devops.paynet.repository.RecipientFieldRepository;
import com.devops.paynet.repository.RecipientRepository;
import com.devops.paynet.web.rest.errors.ResourceNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecipientFieldServiceTest {

    @Mock
    private RecipientFieldRepository fieldRepository;

    @Mock
    private RecipientRepository recipientRepository;

    private final RecipientFieldMapper mapper = new RecipientFieldMapper() {};

    @Test
    void savesFieldWithRecipientLoadedFromDatabase() {
        Recipient recipient = new Recipient();
        recipient.setId(10L);
        recipient.setName("Mobile");
        when(recipientRepository.findById(10L)).thenReturn(Optional.of(recipient));
        when(fieldRepository.save(any(RecipientField.class))).thenAnswer(invocation -> {
            RecipientField field = invocation.getArgument(0);
            field.setId(20L);
            return field;
        });

        var response = service().save(request(10L));

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.recipientId()).isEqualTo(10L);
        verify(fieldRepository).save(org.mockito.ArgumentMatchers.argThat(field -> field.getRecipient() == recipient));
    }

    @Test
    void returnsNotFoundBeforeSavingForUnknownRecipient() {
        when(recipientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().save(request(99L))).isInstanceOf(ResourceNotFoundException.class);
        verify(fieldRepository, never()).save(any());
    }

    private RecipientFieldService service() {
        return new RecipientFieldService(fieldRepository, recipientRepository, mapper);
    }

    private RecipientFieldCreateDto request(Long recipientId) {
        return new RecipientFieldCreateDto(
            "account",
            "Account",
            null,
            RecipientFieldMethod.CHECK,
            true,
            true,
            RecipientFieldType.TEXT,
            1,
            recipientId
        );
    }
}
