package com.devops.paynet.service;

import com.devops.paynet.domain.RecipientField;
import com.devops.paynet.repository.RecipientFieldRepository;
import com.devops.paynet.dto.RecipientFieldCreateDto;
import com.devops.paynet.dto.RecipientFieldResponseDto;
import com.devops.paynet.mapper.RecipientFieldMapper;
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
    private final RecipientFieldMapper recipientFieldMapper;

    public RecipientFieldService(
        RecipientFieldRepository recipientFieldRepository,
        RecipientFieldMapper recipientFieldMapper
    ) {
        this.recipientFieldRepository = recipientFieldRepository;
        this.recipientFieldMapper = recipientFieldMapper;
    }

    /**
     * Save a recipientField.
     *
     * @param recipientFieldCreateDTO the entity to save.
     * @return the persisted entity.
     */
    public RecipientFieldResponseDto save(RecipientFieldCreateDto recipientFieldCreateDTO) {
        LOG.debug("Request to save RecipientField : {}", recipientFieldCreateDTO);
        RecipientField recipientField = recipientFieldMapper.toEntity(
                recipientFieldCreateDTO
        );

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
    public Page<RecipientFieldResponseDto> findAll(Pageable pageable) {
        LOG.debug("Request to get all RecipientFields");
        return recipientFieldRepository
                .findAll(pageable)
                .map(recipientFieldMapper::toResponseDto);
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
                .orElseThrow(() -> new RuntimeException("RecipientField not found with id " + id));
    }
}
