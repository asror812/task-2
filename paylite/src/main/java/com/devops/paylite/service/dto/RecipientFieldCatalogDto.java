package com.devops.paylite.service.dto;

import java.util.List;

public record RecipientFieldCatalogDto(
    Long id,
    String name,
    String label,
    String validationRegex,
    Boolean required,
    String type,
    List<RecipientFieldValueCatalogDto> values
) {}
