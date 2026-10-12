package com.devops.paynet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecipientFieldValueCreateDto(
    @NotBlank @Size(max = 100) String value,
    @NotBlank @Size(max = 150) String label,
    @NotNull Boolean active,
    @NotNull Integer sortOrder,
    @NotNull Long recipientFieldId,
    Long parentId
) {}
