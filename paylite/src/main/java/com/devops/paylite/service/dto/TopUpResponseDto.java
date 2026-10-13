package com.devops.paylite.service.dto;

import java.time.Instant;

public record TopUpResponseDto(
    Long id,
    Long agentId,
    Long amount,
    Long balanceBefore,
    Long balanceAfter,
    Instant createdAt
) {}
