package com.devops.paylite.service.dto;

import com.devops.paylite.domain.enumeration.SyncStatus;
import java.time.Instant;

public record PaynetCatalogSyncResponseDto(
    Long id,
    SyncStatus status,
    Instant startedAt,
    Instant finishedAt,
    Long categoriesProcessed,
    Long recipientsProcessed,
    Long fieldsProcessed,
    Long valuesProcessed,
    String errorMessage
) {}
