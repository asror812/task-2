package com.devops.paynet.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link com.devops.paynet.domain.Category} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public record CategoryCreateDto(
        Long id,
        @NotNull @Size(max = 100) String name,
        @NotNull Boolean active,
        @NotNull Integer sortOrder
) {
}
