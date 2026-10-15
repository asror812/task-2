package com.devops.paynet.service;

import com.devops.paynet.domain.RecipientField;
import com.devops.paynet.domain.Recipient;
import com.devops.paynet.repository.RecipientFieldRepository;
import com.devops.paynet.repository.RecipientRepository;
import com.devops.paynet.dto.RecipientFieldCreateDto;
import com.devops.paynet.dto.RecipientFieldResponseDto;
import com.devops.paynet.mapper.RecipientFieldMapper;
import com.devops.paynet.web.rest.errors.ResourceNotFoundException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecipientFieldService {

    private static final Logger LOG = LoggerFactory.getLogger(RecipientFieldService.class);

    private final RecipientFieldRepository recipientFieldRepository;
    private final RecipientRepository recipientRepository;
    private final RecipientFieldMapper recipientFieldMapper;

    public RecipientFieldService(
        RecipientFieldRepository recipientFieldRepository,
        RecipientRepository recipientRepository,
        RecipientFieldMapper recipientFieldMapper
    ) {
        this.recipientFieldRepository = recipientFieldRepository;
        this.recipientRepository = recipientRepository;
        this.recipientFieldMapper = recipientFieldMapper;
    }

    /**
     * Save a recipientField.
     *
     * @param recipientFieldCreateDTO the entity to save.
     * @return the persisted entity.
     */
    @Transactional
    public RecipientFieldResponseDto save(RecipientFieldCreateDto recipientFieldCreateDTO) {
        LOG.debug("Request to save RecipientField : {}", recipientFieldCreateDTO);
        Recipient recipient = recipientRepository.findById(recipientFieldCreateDTO.recipientId())
            .orElseThrow(() -> new ResourceNotFoundException("Recipient", recipientFieldCreateDTO.recipientId()));
        RecipientField recipientField = recipientFieldMapper.toEntity(
                recipientFieldCreateDTO
        );
        recipientField.setRecipient(recipient);

        recipientField = recipientFieldRepository.save(recipientField);
        return recipientFieldMapper.toResponseDto(recipientField);
    }


    /**
     * Get all the recipientFields.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<RecipientFieldResponseDto> findAll(Pageable pageable, Instant updatedSince) {
        LOG.debug("Request to get all RecipientFields");
        Page<RecipientField> fields = updatedSince == null
            ? recipientFieldRepository.findAllWithEagerRelationships(pageable)
            : recipientFieldRepository.findAllUpdatedSince(updatedSince, pageable);
        return fields.map(recipientFieldMapper::toResponseDto);
    }

    /**
     * Get one recipientField by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public RecipientFieldResponseDto findOne(Long id) {
        LOG.debug("Request to get RecipientField : {}", id);

        return recipientFieldRepository
                .findOneWithEagerRelationships(id)
                .map(recipientFieldMapper::toResponseDto)
                .orElseThrow(() -> new ResourceNotFoundException("Recipient field", id));
    }

    @Transactional
    public RecipientFieldResponseDto updateActive(Long id, boolean active) {
        RecipientField field = recipientFieldRepository.findOneWithEagerRelationships(id)
            .orElseThrow(() -> new ResourceNotFoundException("Recipient field", id));
        field.setActive(active);
        return recipientFieldMapper.toResponseDto(recipientFieldRepository.save(field));
    }
}
