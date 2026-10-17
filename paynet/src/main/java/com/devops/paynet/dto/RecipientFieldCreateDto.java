package com.devops.paynet.dto;

import com.devops.paynet.domain.enumeration.RecipientFieldType;
import jakarta.validation.constraints.*;

@SuppressWarnings("common-java:DuplicatedBlocks")
public record RecipientFieldCreateDto(
        @NotNull(message = "Name is required")
        @Size(max = 50, message = "Name must be at most 50 characters")
        String name,

        @NotNull(message = "Label is required")
        @Size(max = 100, message = "Label must be at most 100 characters")
        String label,

        @Size(max = 255, message = "Validation regex must be at most 255 characters")
        String validationRegex,

        @NotNull(message = "Active status is required")
        Boolean active,

        @NotNull(message = "Required status is required")
        Boolean required,

        @NotNull(message = "Type is required")
        RecipientFieldType type,

        @NotNull(message = "Sort order is required")
        Integer sortOrder,

        @NotNull(message = "Recipient ID is required")
        Long recipientId
        ) {
}
