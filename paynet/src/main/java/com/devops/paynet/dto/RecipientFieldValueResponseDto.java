package com.devops.paynet.dto;

public record RecipientFieldValueResponseDto(
    Long id,
    String value,
    String label,
    Boolean active,
    Integer sortOrder,
    Long recipientFieldId,
    Long parentId
) {}
