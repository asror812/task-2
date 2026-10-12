package com.devops.paylite.service.dto;

public record RecipientFieldValueCatalogDto(
    Long id,
    String value,
    String label,
    Boolean active,
    Integer sortOrder,
    Long recipientFieldId,
    Long parentId
) {}
