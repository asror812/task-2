package com.devops.paylite.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TopUpRequestDto(
    @NotNull @Positive Long amount,
    @NotBlank @Size(max = 100) String idempotencyKey
) {}
