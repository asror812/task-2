package com.devops.paylite.service;

import com.devops.paylite.domain.PaynetCatalogSync;
import com.devops.paylite.domain.enumeration.SyncStatus;
import com.devops.paylite.repository.PaynetCatalogSyncRepository;
import com.devops.paylite.service.dto.PaynetCatalogSyncResponseDto;
import com.devops.paylite.service.dto.load.PaynetCategoryLoadDto;
import com.devops.paylite.service.dto.load.PaynetCatalogEntryLoadDto;
import com.devops.paylite.service.dto.load.PaynetRecipientLoadDto;
import com.devops.paylite.service.dto.load.PaynetRecipientFieldLoadDto;
import com.devops.paylite.web.rest.PaynetServiceClient;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PaynetCatalogSyncService {

    private final PaynetServiceClient paynetServiceClient;
    private final PaynetCatalogWriter paynetCatalogWriter;
    private final PaynetCatalogSyncRepository paynetCatalogSyncRepository;

    @Transactional(readOnly = true)
    public Optional<PaynetCatalogSyncResponseDto> getLatest() {
        return paynetCatalogSyncRepository.findTopByOrderByStartedAtDesc().map(this::toResponse);
    }

    public PaynetCatalogSyncResponseDto sync() {
        Instant startedAt = Instant.now();
        paynetCatalogSyncRepository.failStaleRunning(startedAt.minusSeconds(3600), startedAt);

        PaynetCatalogSync run;
        try {
            run = paynetCatalogSyncRepository.saveAndFlush(
                PaynetCatalogSync.builder()
                    .startedAt(startedAt)
                    .status(SyncStatus.RUNNING)
                    .categoriesProcessed(0L)
                    .recipientsProcessed(0L)
                    .fieldsProcessed(0L)
                    .valuesProcessed(0L)
                    .build()
            );
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Paynet catalog synchronization is already running", exception);
        }

        try {
            List<PaynetCatalogEntryLoadDto> catalog = paynetServiceClient.getCatalog();
            if (catalog == null) {
                throw new IllegalStateException("Paynet returned a null catalog");
            }

            Map<Long, PaynetCategoryLoadDto> categoriesById = new LinkedHashMap<>();
            List<PaynetRecipientLoadDto> recipients = new java.util.ArrayList<>();
            List<PaynetRecipientFieldLoadDto> fields = new java.util.ArrayList<>();
            for (PaynetCatalogEntryLoadDto entry : catalog) {
                if (entry.getCategory() == null || entry.getRecipient() == null) {
                    throw new IllegalStateException("Paynet returned an incomplete catalog entry");
                }
                categoriesById.putIfAbsent(entry.getCategory().getId(), entry.getCategory());
                recipients.add(entry.getRecipient());
                if (entry.getFields() != null) {
                    fields.addAll(entry.getFields());
                }
            }
            List<PaynetCategoryLoadDto> categories = List.copyOf(categoriesById.values());

            if (categories.isEmpty() || recipients.isEmpty()) {
                throw new IllegalStateException("Paynet returned an empty categories or recipients catalog");
            }

            paynetCatalogWriter.writeCatalog(categories, recipients, fields, Instant.now());

            run.setStatus(SyncStatus.SUCCESS);
            run.setFinishedAt(Instant.now());
            run.setCategoriesProcessed((long) categories.size());
            run.setRecipientsProcessed((long) recipients.size());
            run.setFieldsProcessed((long) fields.size());
            run.setValuesProcessed(countValues(fields));
            run = paynetCatalogSyncRepository.save(run);
            return toResponse(run);
        } catch (RuntimeException exception) {
            run.setStatus(SyncStatus.FAILED);
            run.setFinishedAt(Instant.now());
            run.setErrorMessage(limitErrorMessage(exception.getMessage()));
            paynetCatalogSyncRepository.save(run);
            throw exception;
        }
    }

    private long countValues(List<PaynetRecipientFieldLoadDto> fields) {
        return fields
            .stream()
            .map(PaynetRecipientFieldLoadDto::getValues)
            .filter(java.util.Objects::nonNull)
            .mapToLong(List::size)
            .sum();
    }

    private PaynetCatalogSyncResponseDto toResponse(PaynetCatalogSync run) {
        return new PaynetCatalogSyncResponseDto(
            run.getId(),
            run.getStatus(),
            run.getStartedAt(),
            run.getFinishedAt(),
            run.getCategoriesProcessed(),
            run.getRecipientsProcessed(),
            run.getFieldsProcessed(),
            run.getValuesProcessed(),
            run.getErrorMessage()
        );
    }

    private String limitErrorMessage(String message) {
        if (message == null) {
            return "Unknown synchronization error";
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
