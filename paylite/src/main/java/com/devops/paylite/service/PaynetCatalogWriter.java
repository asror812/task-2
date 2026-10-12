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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaynetCatalogWriter {

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
        Map<Long, PaynetCategory> existingByPaynetId = paynetCategoryRepository
            .findAll()
            .stream()
            .collect(Collectors.toMap(PaynetCategory::getPaynetId, Function.identity()));

        Set<Long> incomingIds = categories.stream().map(PaynetCategoryLoadDto::getId).collect(Collectors.toSet());
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

        existingByPaynetId
            .values()
            .stream()
            .filter(entity -> !incomingIds.contains(entity.getPaynetId()))
            .forEach(entity -> {
                entity.setActive(false);
                entity.setLastSyncedAt(syncedAt);
                batch.add(entity);
            });

        return paynetCategoryRepository
            .saveAll(batch)
            .stream()
            .collect(Collectors.toMap(PaynetCategory::getPaynetId, Function.identity()));
    }

    private Map<Long, PaynetRecipient> writeRecipients(
        List<PaynetRecipientLoadDto> recipients,
        Map<Long, PaynetCategory> categoriesByPaynetId,
        Instant syncedAt
    ) {
        Map<Long, PaynetRecipient> existingByPaynetId = paynetRecipientRepository
            .findAll()
            .stream()
            .collect(Collectors.toMap(PaynetRecipient::getPaynetId, Function.identity()));

        Set<Long> incomingIds = new HashSet<>();
        List<PaynetRecipient> batch = new ArrayList<>();

        for (PaynetRecipientLoadDto dto : recipients) {
            PaynetCategory category = categoriesByPaynetId.get(dto.getCategoryId());
            if (category == null) {
                throw new IllegalStateException(
                    "Paynet category " + dto.getCategoryId() + " was not found for recipient " + dto.getId()
                );
            }

            incomingIds.add(dto.getId());
            PaynetRecipient entity = existingByPaynetId.getOrDefault(dto.getId(), new PaynetRecipient());
            entity.setPaynetId(dto.getId());
            entity.setName(dto.getName());
            entity.setCode(dto.getCode());
            entity.setActive(dto.getActive());
            entity.setCategory(category);
            entity.setLastSyncedAt(syncedAt);
            batch.add(entity);
        }

        existingByPaynetId
            .values()
            .stream()
            .filter(entity -> !incomingIds.contains(entity.getPaynetId()))
            .forEach(entity -> {
                entity.setActive(false);
                entity.setLastSyncedAt(syncedAt);
                batch.add(entity);
            });

        return paynetRecipientRepository
            .saveAll(batch)
            .stream()
            .collect(Collectors.toMap(PaynetRecipient::getPaynetId, Function.identity()));
    }

    private Map<Long, PaynetRecipientField> writeFields(
        List<PaynetRecipientFieldLoadDto> fields,
        Map<Long, PaynetRecipient> recipientsByPaynetId,
        Instant syncedAt
    ) {
        Map<Long, PaynetRecipientField> existingByPaynetId = paynetRecipientFieldRepository
            .findAll()
            .stream()
            .collect(Collectors.toMap(PaynetRecipientField::getPaynetId, Function.identity()));

        Set<Long> incomingIds = new HashSet<>();
        List<PaynetRecipientField> batch = new ArrayList<>();

        for (PaynetRecipientFieldLoadDto dto : fields) {
            PaynetRecipient recipient = recipientsByPaynetId.get(dto.getRecipientId());
            if (recipient == null) {
                throw new IllegalStateException(
                    "Paynet recipient " + dto.getRecipientId() + " was not found for field " + dto.getId()
                );
            }

            incomingIds.add(dto.getId());
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

        existingByPaynetId
            .values()
            .stream()
            .filter(entity -> !incomingIds.contains(entity.getPaynetId()))
            .forEach(entity -> {
                entity.setActive(false);
                entity.setLastSyncedAt(syncedAt);
                batch.add(entity);
            });

        return paynetRecipientFieldRepository
            .saveAll(batch)
            .stream()
            .collect(Collectors.toMap(PaynetRecipientField::getPaynetId, Function.identity()));
    }

    private void writeFieldValues(
        List<PaynetRecipientFieldLoadDto> fields,
        Map<Long, PaynetRecipientField> fieldsByPaynetId,
        Instant syncedAt
    ) {
        Map<Long, PaynetRecipientFieldValue> existingByPaynetId = paynetRecipientFieldValueRepository
            .findAll()
            .stream()
            .collect(Collectors.toMap(PaynetRecipientFieldValue::getPaynetId, Function.identity()));

        Set<Long> incomingIds = new HashSet<>();
        Map<Long, Long> parentIdByValueId = new java.util.HashMap<>();
        List<PaynetRecipientFieldValue> batch = new ArrayList<>();

        for (PaynetRecipientFieldLoadDto fieldDto : fields) {
            PaynetRecipientField field = fieldsByPaynetId.get(fieldDto.getId());
            List<PaynetRecipientFieldValueLoadDto> values = fieldDto.getValues() == null ? List.of() : fieldDto.getValues();

            for (PaynetRecipientFieldValueLoadDto dto : values) {
                incomingIds.add(dto.getId());
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

        existingByPaynetId
            .values()
            .stream()
            .filter(entity -> !incomingIds.contains(entity.getPaynetId()))
            .forEach(entity -> {
                entity.setActive(false);
                entity.setLastSyncedAt(syncedAt);
                batch.add(entity);
            });

        Map<Long, PaynetRecipientFieldValue> savedByPaynetId = paynetRecipientFieldValueRepository
            .saveAllAndFlush(batch)
            .stream()
            .collect(Collectors.toMap(PaynetRecipientFieldValue::getPaynetId, Function.identity()));

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
}
