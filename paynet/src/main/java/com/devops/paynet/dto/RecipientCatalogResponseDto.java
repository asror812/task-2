package com.devops.paynet.dto;

import java.util.List;

public record RecipientCatalogResponseDto(
    CategoryResponseDto category,
    RecipientResponseDto recipient,
    List<RecipientFieldResponseDto> fields
) {}
