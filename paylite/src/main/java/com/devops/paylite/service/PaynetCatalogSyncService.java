package com.devops.paylite.service;

import com.devops.paylite.domain.PaynetCatalogSync;
import com.devops.paylite.domain.enumeration.SyncStatus;
import com.devops.paylite.repository.PaynetCatalogSyncRepository;
import com.devops.paylite.service.dto.PaynetCatalogSyncResponseDto;
import com.devops.paylite.service.dto.load.PaynetCategoryLoadDto;
import com.devops.paylite.service.dto.load.PaynetRecipientLoadDto;
import com.devops.paylite.service.dto.load.PaynetRecipientFieldLoadDto;
import com.devops.paylite.web.rest.PaynetServiceClient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaynetCatalogSyncService {

    private static final int PAGE_SIZE = 100;

    private final PaynetServiceClient paynetServiceClient;
    private final PaynetCatalogWriter paynetCatalogWriter;
    private final PaynetCatalogSyncRepository paynetCatalogSyncRepository;

    @Transactional(readOnly = true)
    public Optional<PaynetCatalogSyncResponseDto> getLatest() {
        return paynetCatalogSyncRepository.findTopByOrderByStartedAtDesc().map(this::toResponse);
    }

    public PaynetCatalogSyncResponseDto sync() {
        PaynetCatalogSync run = paynetCatalogSyncRepository.save(
            PaynetCatalogSync.builder()
                .startedAt(Instant.now())
                .status(SyncStatus.RUNNING)
                .categoriesProcessed(0L)
                .recipientsProcessed(0L)
                .fieldsProcessed(0L)
                .valuesProcessed(0L)
                .build()
        );

        try {
            List<PaynetCategoryLoadDto> categories = loadAllCategories();
            List<PaynetRecipientLoadDto> recipients = loadAllRecipients();
            List<PaynetRecipientFieldLoadDto> fields = loadAllRecipientFields();

            if (categories.isEmpty() || recipients.isEmpty() || fields.isEmpty()) {
                throw new IllegalStateException("Paynet returned an empty catalog");
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

    private List<PaynetCategoryLoadDto> loadAllCategories() {
        List<PaynetCategoryLoadDto> result = new ArrayList<>();
        int page = 0;

        while (true) {
            List<PaynetCategoryLoadDto> content = paynetServiceClient.getCategoryPage(page, PAGE_SIZE);
            if (content == null) {
                throw new IllegalStateException("Paynet returned null categories page " + page);
            }
            result.addAll(content);
            if (content.size() < PAGE_SIZE) {
                return result;
            }
            page++;
        }
    }

    private List<PaynetRecipientLoadDto> loadAllRecipients() {
        List<PaynetRecipientLoadDto> result = new ArrayList<>();
        int page = 0;

        while (true) {
            List<PaynetRecipientLoadDto> content = paynetServiceClient.getRecipientPage(page, PAGE_SIZE);
            if (content == null) {
                throw new IllegalStateException("Paynet returned null recipients page " + page);
            }
            result.addAll(content);
            if (content.size() < PAGE_SIZE) {
                return result;
            }
            page++;
        }
    }

    private List<PaynetRecipientFieldLoadDto> loadAllRecipientFields() {
        List<PaynetRecipientFieldLoadDto> result = new ArrayList<>();
        int page = 0;

        while (true) {
            List<PaynetRecipientFieldLoadDto> content = paynetServiceClient.getRecipientFields(page, PAGE_SIZE);
            if (content == null) {
                throw new IllegalStateException("Paynet returned null recipient fields page " + page);
            }
            result.addAll(content);
            if (content.size() < PAGE_SIZE) {
                return result;
            }
            page++;
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
