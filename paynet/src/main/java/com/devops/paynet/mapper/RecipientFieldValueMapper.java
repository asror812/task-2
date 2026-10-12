package com.devops.paynet.mapper;

import com.devops.paynet.domain.RecipientField;
import com.devops.paynet.domain.RecipientFieldValue;
import com.devops.paynet.dto.RecipientFieldValueCreateDto;
import com.devops.paynet.dto.RecipientFieldValueResponseDto;
import org.springframework.stereotype.Component;

@Component
public class RecipientFieldValueMapper {

    public RecipientFieldValue toEntity(RecipientFieldValueCreateDto dto) {
        RecipientFieldValue entity = new RecipientFieldValue();
        entity.setValue(dto.value());
        entity.setLabel(dto.label());
        entity.setActive(dto.active());
        entity.setSortOrder(dto.sortOrder());

        RecipientField field = new RecipientField();
        field.setId(dto.recipientFieldId());
        entity.setRecipientField(field);

        if (dto.parentId() != null) {
            RecipientFieldValue parent = new RecipientFieldValue();
            parent.setId(dto.parentId());
            entity.setParent(parent);
        }
        return entity;
    }

    public RecipientFieldValueResponseDto toResponseDto(RecipientFieldValue entity) {
        return new RecipientFieldValueResponseDto(
            entity.getId(),
            entity.getValue(),
            entity.getLabel(),
            entity.getActive(),
            entity.getSortOrder(),
            entity.getRecipientField().getId(),
            entity.getParent() == null ? null : entity.getParent().getId()
        );
    }
}
