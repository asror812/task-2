package com.devops.paynet.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devops.paynet.domain.RecipientField;
import com.devops.paynet.domain.enumeration.RecipientFieldType;
import com.devops.paynet.dto.RecipientFieldValueCreateDto;
import com.devops.paynet.mapper.RecipientFieldValueMapper;
import com.devops.paynet.repository.RecipientFieldRepository;
import com.devops.paynet.repository.RecipientFieldValueRepository;
import com.devops.paynet.web.rest.errors.BadRequestAlertException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecipientFieldValueServiceTest {

    @Mock
    private RecipientFieldValueRepository valueRepository;

    @Mock
    private RecipientFieldRepository fieldRepository;

    @Test
    void rejectsOptionForNonSelectField() {
        RecipientField field = new RecipientField();
        field.setId(10L);
        field.setType(RecipientFieldType.TEXT);
        when(fieldRepository.findOneWithEagerRelationships(10L)).thenReturn(Optional.of(field));

        assertThatThrownBy(() -> service().save(new RecipientFieldValueCreateDto("1", "One", true, 1, 10L, null)))
            .isInstanceOf(BadRequestAlertException.class);
        verify(valueRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private RecipientFieldValueService service() {
        return new RecipientFieldValueService(valueRepository, fieldRepository, new RecipientFieldValueMapper());
    }
}
