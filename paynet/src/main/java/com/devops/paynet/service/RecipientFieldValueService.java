package com.devops.paynet.service;

import com.devops.paynet.domain.RecipientFieldValue;
import com.devops.paynet.repository.RecipientFieldRepository;
import com.devops.paynet.repository.RecipientFieldValueRepository;
import com.devops.paynet.dto.RecipientFieldValueCreateDto;
import com.devops.paynet.dto.RecipientFieldValueResponseDto;
import com.devops.paynet.mapper.RecipientFieldValueMapper;
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
        var field = fieldRepository.findById(dto.recipientFieldId())
            .orElseThrow(() -> new IllegalArgumentException("Recipient field not found: " + dto.recipientFieldId()));

        if (dto.parentId() != null) {
            RecipientFieldValue parent = repository.findById(dto.parentId())
                .orElseThrow(() -> new IllegalArgumentException("Parent field value not found: " + dto.parentId()));
            if (!parent.getRecipientField().getRecipient().getId().equals(field.getRecipient().getId())) {
                throw new IllegalArgumentException("Parent field value belongs to another recipient");
            }
        }

        return mapper.toResponseDto(repository.save(mapper.toEntity(dto)));
    }

    @Transactional(readOnly = true)
    public RecipientFieldValueResponseDto findOne(Long id) {
        return repository.findById(id)
            .map(mapper::toResponseDto)
            .orElseThrow(() -> new IllegalArgumentException("Recipient field value not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<RecipientFieldValueResponseDto> findOptions(Long recipientFieldId, Long parentId) {
        List<RecipientFieldValue> values = parentId == null
            ? repository.findAllByRecipientField_IdAndParentIsNullAndActiveTrueOrderBySortOrderAsc(recipientFieldId)
            : repository.findAllByRecipientField_IdAndParent_IdAndActiveTrueOrderBySortOrderAsc(recipientFieldId, parentId);
        return values.stream().map(mapper::toResponseDto).toList();
    }
}
