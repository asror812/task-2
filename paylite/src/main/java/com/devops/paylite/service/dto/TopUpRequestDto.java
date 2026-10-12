package com.devops.paylite.service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TopUpRequestDto(@NotNull @Positive Long amount) {}
