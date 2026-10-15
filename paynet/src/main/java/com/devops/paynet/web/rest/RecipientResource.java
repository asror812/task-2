package com.devops.paynet.web.rest;

import com.devops.paynet.security.AuthoritiesConstants;
import com.devops.paynet.service.RecipientService;
import com.devops.paynet.dto.CheckRequestDto;
import com.devops.paynet.dto.CheckResponseDto;
import com.devops.paynet.dto.PayRequestDto;
import com.devops.paynet.dto.PayResponseDto;
import com.devops.paynet.dto.RecipientCreateDto;
import com.devops.paynet.dto.RecipientResponseDto;
import com.devops.paynet.dto.RecipientCatalogResponseDto;
import com.devops.paynet.dto.ActiveUpdateDto;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
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
    @PreAuthorize("hasAuthority('" + AuthoritiesConstants.ADMIN + "')")
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
    public ResponseEntity<List<RecipientResponseDto>> getAllRecipients(
        @ParameterObject Pageable pageable,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant updatedSince
    ) {
        LOG.debug("REST request to get a page of Recipients");

        Page<RecipientResponseDto> page = recipientService.findAll(pageable, updatedSince);

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(
                ServletUriComponentsBuilder.fromCurrentRequest(),
                page
        );

        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/catalog")
    public List<RecipientCatalogResponseDto> getCatalog(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant updatedSince
    ) {
        return recipientService.findCatalog(updatedSince);
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
            @Valid @RequestBody CheckRequestDto checkRequestDto){
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

    @PreAuthorize("hasAuthority('" + AuthoritiesConstants.ADMIN + "')")
    @PatchMapping("/{id}/active")
    public ResponseEntity<RecipientResponseDto> updateActive(
        @PathVariable Long id,
        @Valid @RequestBody ActiveUpdateDto dto
    ) {
        return ResponseEntity.ok(recipientService.updateActive(id, dto.active()));
    }

}
