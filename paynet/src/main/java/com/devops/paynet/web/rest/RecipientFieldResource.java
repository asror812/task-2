package com.devops.paynet.web.rest;

import com.devops.paynet.service.RecipientFieldService;
import com.devops.paynet.dto.RecipientFieldCreateDto;
import com.devops.paynet.dto.RecipientFieldResponseDto;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;

@RestController
@RequestMapping("/api/recipient-fields")
public class RecipientFieldResource {

    private static final Logger LOG = LoggerFactory.getLogger(
            RecipientFieldResource.class
    );

    private static final String ENTITY_NAME = "paynetRecipientField";

    @Value("${jhipster.clientApp.name:paynet}")
    private String applicationName;

    private final RecipientFieldService recipientFieldService;

    public RecipientFieldResource(RecipientFieldService recipientFieldService) {
        this.recipientFieldService = recipientFieldService;
    }

    /**
     * {@code POST  /recipient-fields} : Create a new recipientField.
     *
     * @param recipientFieldCreateDTO the recipientFieldCreateDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new recipientFieldCreateDTO, or with status {@code 400 (Bad Request)} if the recipientField already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<RecipientFieldResponseDto> createRecipientField(@Valid @RequestBody RecipientFieldCreateDto recipientFieldCreateDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to save RecipientField : {}", recipientFieldCreateDTO);

        RecipientFieldResponseDto dto = recipientFieldService.save(recipientFieldCreateDTO);

        return ResponseEntity.created(
                        new URI("/api/recipient-fields/" + dto.id())
                )
                .headers(
                        HeaderUtil.createEntityCreationAlert(
                                applicationName,
                                false,
                                ENTITY_NAME,
                                dto.id().toString()
                        )
                )
                .body(dto);
    }

    /**
     * {@code GET  /recipient-fields} : get all the Recipient Fields.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Recipient Fields in body.
     */
    @GetMapping
    public ResponseEntity<List<RecipientFieldResponseDto>> getAll(
            @ParameterObject Pageable pageable) {

        LOG.debug("REST request to get a page of RecipientFields");

        Page<RecipientFieldResponseDto> page = recipientFieldService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(
                ServletUriComponentsBuilder.fromCurrentRequest(),
                page
        );

        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /recipient-fields/:id} : get the "id" recipientField.
     *
     * @param id the id of the recipientFieldDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the recipientFieldDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RecipientFieldResponseDto> getRecipientField(@PathVariable("id") Long id) {
        LOG.debug("REST request to get RecipientField : {}", id);

        RecipientFieldResponseDto recipientFieldDTO = recipientFieldService.findOne(id);
        return ResponseEntity.ok().body(recipientFieldDTO);
    }
}
