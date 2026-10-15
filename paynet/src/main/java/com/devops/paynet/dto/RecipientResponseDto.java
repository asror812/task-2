package com.devops.paynet.dto;

import java.time.Instant;

/**
 * A DTO for the {@link com.devops.paynet.domain.Recipient} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public record RecipientResponseDto(
        Long id,
        String name,
        String code,
        Boolean active,
        String categoryName,
        Long categoryId,
        Instant updatedAt
) {
}
