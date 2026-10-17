package com.devops.paylite.service.dto;

public record RecipientFieldValueCatalogDto(
    Long id,
    String value,
    String label,
    Long parentId
) {}
