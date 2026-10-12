package com.devops.paynet.web.rest;

import com.devops.paynet.security.AuthoritiesConstants;
import com.devops.paynet.service.RecipientService;
import com.devops.paynet.dto.CheckRequestDto;
import com.devops.paynet.dto.CheckResponseDto;
import com.devops.paynet.dto.PayRequestDto;
import com.devops.paynet.dto.PayResponseDto;
import com.devops.paynet.dto.RecipientCreateDto;
import com.devops.paynet.dto.RecipientResponseDto;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;


@RestController
@RequestMapping("/api/recipients")
public class RecipientResource {

    private static final Logger LOG = LoggerFactory.getLogger(RecipientResource.class);
    private static final String ENTITY_NAME = "paynetRecipient";

    @Value("${jhipster.clientApp.name:paynet}")
    private String applicationName;
    private final RecipientService recipientService;

    public RecipientResource(RecipientService recipientService) {
        this.recipientService = recipientService;
    }

    /**
     * {@code POST  /recipients} : Create a new recipient.
     *
     * @param recipientDTO the recipientDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new recipientDTO, or with status {@code 400 (Bad Request)} if the recipient already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping
    public ResponseEntity<RecipientResponseDto> createRecipient(@Valid @RequestBody RecipientCreateDto recipientDTO) throws URISyntaxException {
        LOG.debug("REST request to save Recipient : {}", recipientDTO);

        RecipientResponseDto dto = recipientService.save(recipientDTO);

        return ResponseEntity.created(
                        new URI("/api/recipients/" + dto.id())
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
     * {@code GET  /recipients} : get all the Recipients.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Recipients in body.
     */
    @GetMapping
    public ResponseEntity<List<RecipientResponseDto>> getAllRecipients(@ParameterObject Pageable pageable) {
        LOG.debug("REST request to get a page of Recipients");

        Page<RecipientResponseDto> page = recipientService.findAll(pageable);

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(
                ServletUriComponentsBuilder.fromCurrentRequest(),
                page
        );

        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /recipients/:id} : get the "id" recipient.
     *
     * @param id the id of the recipientDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the recipientDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RecipientResponseDto> getRecipient(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Recipient : {}", id);

        RecipientResponseDto recipientDTO = recipientService.findOne(id);
        return ResponseEntity.ok().body(recipientDTO);
    }

    @PreAuthorize("hasAuthority('" + AuthoritiesConstants.PAYNET_CLIENT + "')")
    @PostMapping("/{recipientId}/check")
    public ResponseEntity<CheckResponseDto> checkRecipient(
            @PathVariable("recipientId") Long recipientId,
            @RequestBody CheckRequestDto checkRequestDto){
        LOG.debug("REST request to check Recipient : {}", recipientId);

        CheckResponseDto checkResponseDto = recipientService.checkRecipient(recipientId, checkRequestDto);
        return ResponseEntity.ok().body(checkResponseDto);
    }

    @PreAuthorize("hasAuthority('" + AuthoritiesConstants.PAYNET_CLIENT + "')")
    @PostMapping("/{recipientId}/pay")
    public ResponseEntity<PayResponseDto> payRecipient(
            @PathVariable("recipientId") Long recipientId,
            @Valid @RequestBody PayRequestDto payRequestDto) {
        LOG.debug("REST request to pay Recipient : {}", recipientId);

        PayResponseDto payResponseDto = recipientService.payRecipient(recipientId, payRequestDto);
        return ResponseEntity.ok().body(payResponseDto);
    }

}
