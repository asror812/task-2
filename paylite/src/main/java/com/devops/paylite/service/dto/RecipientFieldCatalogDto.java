package com.devops.paylite.service.dto;

import java.util.List;

public record RecipientFieldCatalogDto(
    Long id,
    String name,
    String label,
    String validationRegex,
    Boolean active,
    Boolean required,
    String type,
    Integer sortOrder,
    Long recipientId,
    String recipientName,
    String categoryName,
    List<RecipientFieldValueCatalogDto> values
) {}
