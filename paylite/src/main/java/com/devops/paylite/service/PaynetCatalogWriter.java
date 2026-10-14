package com.devops.paylite.service;

import com.devops.paylite.domain.PaynetCategory;
import com.devops.paylite.domain.PaynetRecipient;
import com.devops.paylite.domain.PaynetRecipientField;
import com.devops.paylite.domain.PaynetRecipientFieldValue;
import com.devops.paylite.repository.PaynetCategoryRepository;
import com.devops.paylite.repository.PaynetRecipientFieldRepository;
import com.devops.paylite.repository.PaynetRecipientFieldValueRepository;
import com.devops.paylite.repository.PaynetRecipientRepository;
import com.devops.paylite.service.dto.load.PaynetCategoryLoadDto;
import com.devops.paylite.service.dto.load.PaynetRecipientLoadDto;
import com.devops.paylite.service.dto.load.PaynetRecipientFieldLoadDto;
import com.devops.paylite.service.dto.load.PaynetRecipientFieldValueLoadDto;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaynetCatalogWriter {

    private static final int LOOKUP_BATCH_SIZE = 500;

    private final PaynetRecipientRepository paynetRecipientRepository;
    private final PaynetCategoryRepository paynetCategoryRepository;
    private final PaynetRecipientFieldRepository paynetRecipientFieldRepository;
    private final PaynetRecipientFieldValueRepository paynetRecipientFieldValueRepository;

    @Transactional
    public void writeCatalog(
        List<PaynetCategoryLoadDto> categories,
        List<PaynetRecipientLoadDto> recipients,
        List<PaynetRecipientFieldLoadDto> fields,
        Instant syncedAt
    ) {
        Map<Long, PaynetCategory> categoriesByPaynetId = writeCategories(categories, syncedAt);
        Map<Long, PaynetRecipient> recipientsByPaynetId = writeRecipients(recipients, categoriesByPaynetId, syncedAt);
        Map<Long, PaynetRecipientField> fieldsByPaynetId = writeFields(fields, recipientsByPaynetId, syncedAt);
        writeFieldValues(fields, fieldsByPaynetId, syncedAt);
    }

    private Map<Long, PaynetCategory> writeCategories(List<PaynetCategoryLoadDto> categories, Instant syncedAt) {
        Set<Long> incomingIds = categories.stream()
            .map(PaynetCategoryLoadDto::getId)
            .collect(Collectors.toSet());

        Map<Long, PaynetCategory> existingByPaynetId = findExistingInBatches(
            incomingIds,
            paynetCategoryRepository::findAllByPaynetIdIn
        )
            .stream()
            .collect(Collectors.toMap(PaynetCategory::getPaynetId, Function.identity()));

        List<PaynetCategory> batch = new ArrayList<>();

        for (PaynetCategoryLoadDto dto : categories) {
            PaynetCategory entity = existingByPaynetId.getOrDefault(dto.getId(), new PaynetCategory());
            entity.setPaynetId(dto.getId());
            entity.setName(dto.getName());
            entity.setActive(dto.getActive());
            entity.setSortOrder(dto.getSortOrder());
            entity.setLastSyncedAt(syncedAt);
            batch.add(entity);
        }

        Map<Long, PaynetCategory> savedByPaynetId = paynetCategoryRepository
            .saveAll(batch)
            .stream()
            .collect(Collectors.toMap(PaynetCategory::getPaynetId, Function.identity()));

        paynetCategoryRepository.deactivateNotSyncedAt(syncedAt);
        return savedByPaynetId;
    }

    private Map<Long, PaynetRecipient> writeRecipients(
        List<PaynetRecipientLoadDto> recipients,
        Map<Long, PaynetCategory> categoriesByPaynetId,
        Instant syncedAt
    ) {

        Set<Long> incomingIds = recipients.stream().map(PaynetRecipientLoadDto::getId).collect(Collectors.toSet());
        Map<Long, PaynetRecipient> existingByPaynetId = findExistingInBatches(
            incomingIds,
            paynetRecipientRepository::findAllByPaynetIdIn
        )
            .stream()
            .collect(Collectors.toMap(PaynetRecipient::getPaynetId, Function.identity()));

        List<PaynetRecipient> batch = new ArrayList<>();

        for (PaynetRecipientLoadDto dto : recipients) {
            PaynetCategory category = categoriesByPaynetId.get(dto.getCategoryId());
            if (category == null) {
                throw new IllegalStateException(
                    "Paynet category " + dto.getCategoryId() + " was not found for recipient " + dto.getId()
                );
            }

            PaynetRecipient entity = existingByPaynetId.getOrDefault(dto.getId(), new PaynetRecipient());
            entity.setPaynetId(dto.getId());
            entity.setName(dto.getName());
            entity.setCode(dto.getCode());
            entity.setActive(dto.getActive());
            entity.setCategory(category);
            entity.setLastSyncedAt(syncedAt);
            batch.add(entity);
        }

        Map<Long, PaynetRecipient> savedByPaynetId = paynetRecipientRepository
            .saveAll(batch)
            .stream()
            .collect(Collectors.toMap(PaynetRecipient::getPaynetId, Function.identity()));

        paynetRecipientRepository.deactivateNotSyncedAt(syncedAt);
        return savedByPaynetId;
    }

    private Map<Long, PaynetRecipientField> writeFields(
        List<PaynetRecipientFieldLoadDto> fields,
        Map<Long, PaynetRecipient> recipientsByPaynetId,
        Instant syncedAt
    ) {
        Set<Long> incomingIds = fields.stream().map(PaynetRecipientFieldLoadDto::getId).collect(Collectors.toSet());
        Map<Long, PaynetRecipientField> existingByPaynetId = findExistingInBatches(
            incomingIds,
            paynetRecipientFieldRepository::findAllByPaynetIdIn
        )
            .stream()
            .collect(Collectors.toMap(PaynetRecipientField::getPaynetId, Function.identity()));

        List<PaynetRecipientField> batch = new ArrayList<>();

        for (PaynetRecipientFieldLoadDto dto : fields) {
            PaynetRecipient recipient = recipientsByPaynetId.get(dto.getRecipientId());
            if (recipient == null) {
                throw new IllegalStateException(
                    "Paynet recipient " + dto.getRecipientId() + " was not found for field " + dto.getId()
                );
            }

            PaynetRecipientField entity = existingByPaynetId.getOrDefault(dto.getId(), new PaynetRecipientField());
            entity.setPaynetId(dto.getId());
            entity.setName(dto.getName());
            entity.setLabel(dto.getLabel());
            entity.setValidationRegex(dto.getValidationRegex());
            entity.setMethod(dto.getMethod());
            entity.setActive(dto.getActive());
            entity.setRequired(dto.getRequired());
            entity.setType(dto.getType());
            entity.setSortOrder(dto.getSortOrder());
            entity.setRecipient(recipient);
            entity.setLastSyncedAt(syncedAt);
            batch.add(entity);
        }

        Map<Long, PaynetRecipientField> savedByPaynetId = paynetRecipientFieldRepository
            .saveAll(batch)
            .stream()
            .collect(Collectors.toMap(PaynetRecipientField::getPaynetId, Function.identity()));
        paynetRecipientFieldRepository.deactivateNotSyncedAt(syncedAt);
        return savedByPaynetId;
    }

    private void writeFieldValues(
        List<PaynetRecipientFieldLoadDto> fields,
        Map<Long, PaynetRecipientField> fieldsByPaynetId,
        Instant syncedAt
    ) {
        Set<Long> incomingIds = fields
            .stream()
            .map(PaynetRecipientFieldLoadDto::getValues)
            .filter(java.util.Objects::nonNull)
            .flatMap(List::stream)
            .map(PaynetRecipientFieldValueLoadDto::getId)
            .collect(Collectors.toSet());
        Map<Long, PaynetRecipientFieldValue> existingByPaynetId = findExistingInBatches(
            incomingIds,
            paynetRecipientFieldValueRepository::findAllByPaynetIdIn
        )
            .stream()
            .collect(Collectors.toMap(PaynetRecipientFieldValue::getPaynetId, Function.identity()));

        Map<Long, Long> parentIdByValueId = new java.util.HashMap<>();
        List<PaynetRecipientFieldValue> batch = new ArrayList<>();

        for (PaynetRecipientFieldLoadDto fieldDto : fields) {
            PaynetRecipientField field = fieldsByPaynetId.get(fieldDto.getId());
            List<PaynetRecipientFieldValueLoadDto> values = fieldDto.getValues() == null ? List.of() : fieldDto.getValues();

            for (PaynetRecipientFieldValueLoadDto dto : values) {
                parentIdByValueId.put(dto.getId(), dto.getParentId());

                PaynetRecipientFieldValue entity = existingByPaynetId.getOrDefault(
                    dto.getId(),
                    new PaynetRecipientFieldValue()
                );

                entity.setPaynetId(dto.getId());
                entity.setValue(dto.getValue());
                entity.setLabel(dto.getLabel());
                entity.setActive(dto.getActive());
                entity.setSortOrder(dto.getSortOrder());
                entity.setRecipientField(field);
                entity.setParent(null);
                entity.setLastSyncedAt(syncedAt);
                batch.add(entity);
            }
        }

        Map<Long, PaynetRecipientFieldValue> savedByPaynetId = paynetRecipientFieldValueRepository
            .saveAllAndFlush(batch)
            .stream()
            .collect(Collectors.toMap(PaynetRecipientFieldValue::getPaynetId, Function.identity()));
        paynetRecipientFieldValueRepository.deactivateNotSyncedAt(syncedAt);

        List<PaynetRecipientFieldValue> valuesWithParents = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : parentIdByValueId.entrySet()) {
            PaynetRecipientFieldValue value = savedByPaynetId.get(entry.getKey());
            Long parentPaynetId = entry.getValue();
            if (parentPaynetId != null) {
                if (!incomingIds.contains(parentPaynetId)) {
                    throw new IllegalStateException(
                        "Paynet parent field value " + parentPaynetId + " was not found for value " + entry.getKey()
                    );
                }
                value.setParent(savedByPaynetId.get(parentPaynetId));
            }
            valuesWithParents.add(value);
        }
        paynetRecipientFieldValueRepository.saveAll(valuesWithParents);
    }

    private <T> List<T> findExistingInBatches(
        Set<Long> paynetIds,
        Function<Collection<Long>, List<T>> loader
    ) {
        List<Long> ids = new ArrayList<>(paynetIds);
        List<T> result = new ArrayList<>();
        for (int from = 0; from < ids.size(); from += LOOKUP_BATCH_SIZE) {
            int to = Math.min(from + LOOKUP_BATCH_SIZE, ids.size());
            result.addAll(loader.apply(ids.subList(from, to)));
        }
        return result;
    }
}
