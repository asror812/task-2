package com.devops.paynet.dto;

import jakarta.validation.constraints.*;

/**
 * A DTO for the {@link com.devops.paynet.domain.Recipient} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public record RecipientCreateDto(

  @NotNull(message = "Name is required")
  @Size(max = 100, message = "Name must be at most 100 characters")
  String name,

  @NotNull(message = "Code is required")
  @Size(max = 50, message = "Code must be at most 50 characters")
  String code,

  @NotNull(message = "Active status is required")
  Boolean active,

  @NotNull(message = "Category ID is required")
  Long categoryId
) {
}
