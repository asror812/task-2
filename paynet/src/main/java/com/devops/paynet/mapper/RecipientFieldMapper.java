package com.devops.paynet.mapper;

import com.devops.paynet.domain.Category;
import com.devops.paynet.domain.Recipient;
import com.devops.paynet.domain.RecipientField;
import com.devops.paynet.dto.RecipientFieldCreateDto;
import com.devops.paynet.dto.RecipientFieldResponseDto;
import com.devops.paynet.dto.RecipientFieldValueResponseDto;
import java.util.Comparator;
import java.util.List;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link RecipientField} and its DTO {@link RecipientFieldResponseDto}.
 */
@Mapper(componentModel = "spring")
public interface RecipientFieldMapper {
    default RecipientField toEntity(RecipientFieldCreateDto recipientFieldCreateDTO) {
        if (recipientFieldCreateDTO == null) {
            return null;
        }

        RecipientField recipientField = new RecipientField();
        recipientField.setName(recipientFieldCreateDTO.name());
        recipientField.setLabel(recipientFieldCreateDTO.label());
        recipientField.setValidationRegex(recipientFieldCreateDTO.validationRegex());
        recipientField.setMethod(recipientFieldCreateDTO.method());
        recipientField.setActive(recipientFieldCreateDTO.active());
        recipientField.setRequired(recipientFieldCreateDTO.required());
        recipientField.setType(recipientFieldCreateDTO.type());
        recipientField.setSortOrder(recipientFieldCreateDTO.sortOrder());

        return recipientField;
    }

    default RecipientFieldResponseDto toResponseDto(RecipientField recipientField) {
        if (recipientField == null) {
            return null;
        }

        Recipient recipient = recipientField.getRecipient();
        Category category = recipient != null ? recipient.getCategory() : null;
        List<RecipientFieldValueResponseDto> values = recipientField.getValues() == null
            ? List.of()
            : recipientField.getValues().stream()
                .sorted(Comparator.comparingInt(value -> value.getSortOrder()))
                .map(value -> new RecipientFieldValueResponseDto(
                    value.getId(),
                    value.getValue(),
                    value.getLabel(),
                    value.getActive(),
                    value.getSortOrder(),
                    recipientField.getId(),
                    value.getParent() == null ? null : value.getParent().getId(),
                    value.getLastModifiedDate()
                ))
                .toList();
        return new RecipientFieldResponseDto(
            recipientField.getId(),
            recipientField.getName(),
            recipientField.getLabel(),
            recipientField.getValidationRegex(),
            recipientField.getMethod(),
            recipientField.getActive(),
            recipientField.getRequired(),
            recipientField.getType(),
            recipientField.getSortOrder(),
            recipient != null ? recipient.getId() : null,
            recipient != null ? recipient.getName() : null,
            category != null ? category.getName() : null,
            values,
            recipientField.getLastModifiedDate()
        );
    }
}
