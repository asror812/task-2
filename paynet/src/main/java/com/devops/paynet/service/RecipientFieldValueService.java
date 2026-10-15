package com.devops.paynet.service;

import com.devops.paynet.domain.RecipientFieldValue;
import com.devops.paynet.domain.RecipientField;
import com.devops.paynet.domain.enumeration.RecipientFieldType;
import com.devops.paynet.repository.RecipientFieldRepository;
import com.devops.paynet.repository.RecipientFieldValueRepository;
import com.devops.paynet.dto.RecipientFieldValueCreateDto;
import com.devops.paynet.dto.RecipientFieldValueResponseDto;
import com.devops.paynet.mapper.RecipientFieldValueMapper;
import com.devops.paynet.web.rest.errors.BadRequestAlertException;
import com.devops.paynet.web.rest.errors.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecipientFieldValueService {

    private final RecipientFieldValueRepository repository;
    private final RecipientFieldRepository fieldRepository;
    private final RecipientFieldValueMapper mapper;

    public RecipientFieldValueService(
        RecipientFieldValueRepository repository,
        RecipientFieldRepository fieldRepository,
        RecipientFieldValueMapper mapper
    ) {
        this.repository = repository;
        this.fieldRepository = fieldRepository;
        this.mapper = mapper;
    }

    @Transactional
    public RecipientFieldValueResponseDto save(RecipientFieldValueCreateDto dto) {
        RecipientField field = fieldRepository.findOneWithEagerRelationships(dto.recipientFieldId())
            .orElseThrow(() -> new ResourceNotFoundException("Recipient field", dto.recipientFieldId()));

        if (field.getType() != RecipientFieldType.SELECT) {
            throw new BadRequestAlertException(
                "Options can only be added to SELECT fields",
                "recipientFieldValue",
                "fieldnotselect"
            );
        }

        RecipientFieldValue parent = null;
        if (dto.parentId() != null) {
            parent = repository.findById(dto.parentId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent field value", dto.parentId()));
            if (!parent.getRecipientField().getRecipient().getId().equals(field.getRecipient().getId())) {
                throw new BadRequestAlertException(
                    "Parent field value belongs to another recipient",
                    "recipientFieldValue",
                    "parentrecipientmismatch"
                );
            }
        }

        RecipientFieldValue entity = mapper.toEntity(dto);
        entity.setRecipientField(field);
        entity.setParent(parent);
        return mapper.toResponseDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public RecipientFieldValueResponseDto findOne(Long id) {
        return repository.findById(id)
            .map(mapper::toResponseDto)
            .orElseThrow(() -> new ResourceNotFoundException("Recipient field value", id));
    }

    @Transactional(readOnly = true)
    public List<RecipientFieldValueResponseDto> findOptions(Long recipientFieldId, Long parentId) {
        if (!fieldRepository.existsById(recipientFieldId)) {
            throw new ResourceNotFoundException("Recipient field", recipientFieldId);
        }
        List<RecipientFieldValue> values = parentId == null
            ? repository.findAllByRecipientField_IdAndParentIsNullAndActiveTrueOrderBySortOrderAsc(recipientFieldId)
            : repository.findAllByRecipientField_IdAndParent_IdAndActiveTrueOrderBySortOrderAsc(recipientFieldId, parentId);
        return values.stream().map(mapper::toResponseDto).toList();
    }

    @Transactional(readOnly = true)
    public List<RecipientFieldValueResponseDto> findOptionsBulk(List<Long> recipientFieldIds, Instant updatedSince) {
        if (recipientFieldIds == null || recipientFieldIds.isEmpty()) {
            throw new BadRequestAlertException(
                "recipientFieldIds cannot be empty",
                "recipientFieldValue",
                "fieldidsrequired"
            );
        }
        List<RecipientFieldValue> values = updatedSince == null
            ? repository.findAllByRecipientField_IdInOrderByRecipientField_IdAscSortOrderAsc(recipientFieldIds)
            : repository.findAllByRecipientField_IdInAndLastModifiedDateAfterOrderByRecipientField_IdAscSortOrderAsc(
                recipientFieldIds,
                updatedSince
            );
        return values.stream().map(mapper::toResponseDto).toList();
    }

    @Transactional
    public RecipientFieldValueResponseDto updateActive(Long id, boolean active) {
        RecipientFieldValue value = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Recipient field value", id));
        value.setActive(active);
        return mapper.toResponseDto(repository.save(value));
    }
}
