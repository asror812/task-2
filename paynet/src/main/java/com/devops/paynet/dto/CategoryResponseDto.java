package com.devops.paynet.dto;

import java.time.Instant;

public record CategoryResponseDto(Long id, String name, Boolean active, Integer sortOrder, Instant updatedAt) {}
