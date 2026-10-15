package com.devops.paynet.dto;

import java.time.Instant;

public record RecipientFieldValueResponseDto(
    Long id,
    String value,
    String label,
    Boolean active,
    Integer sortOrder,
    Long recipientFieldId,
    Long parentId,
    Instant updatedAt
) {}
