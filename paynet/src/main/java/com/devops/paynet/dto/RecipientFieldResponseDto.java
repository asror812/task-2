package com.devops.paynet.dto;

import com.devops.paynet.domain.enumeration.RecipientFieldMethod;
import com.devops.paynet.domain.enumeration.RecipientFieldType;
import java.util.List;

@SuppressWarnings("common-java:DuplicatedBlocks")
public record RecipientFieldResponseDto(
        Long id,
        String name,
        String label,
        String validationRegex,
        RecipientFieldMethod method,

        Boolean active,
        Boolean required,

        RecipientFieldType type,
        Integer sortOrder,
        Long recipientId,
        String recipientName,
        String categoryName,
        List<RecipientFieldValueResponseDto> values
        ) {
}
