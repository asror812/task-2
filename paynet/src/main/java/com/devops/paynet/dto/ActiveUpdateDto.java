package com.devops.paynet.dto;

import jakarta.validation.constraints.NotNull;

public record ActiveUpdateDto(@NotNull Boolean active) {}
