package com.devops.paynet.web.rest;

import com.devops.paynet.security.AuthoritiesConstants;
import com.devops.paynet.service.CategoryService;
import com.devops.paynet.dto.CategoryCreateDto;
import com.devops.paynet.dto.CategoryResponseDto;
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
@RequestMapping("/api/categories")
public class CategoryResource {

  private static final Logger LOG = LoggerFactory.getLogger(CategoryResource.class);
  private static final String ENTITY_NAME = "paynetCategory";

  @Value("${jhipster.clientApp.name:paynet}")
  private String applicationName;

  private final CategoryService categoryService;

  public CategoryResource(CategoryService categoryService) {
    this.categoryService = categoryService;
  }

  /**
   * {@code POST  /categories} : Create a new category.
   *
   * @param dto the categoryDTO to create.
   * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new categoryDTO, or with status {@code 400 (Bad Request)} if the category already has an ID.
   * @throws URISyntaxException if the Location URI syntax is incorrect.
   */
  @PreAuthorize("hasAuthority('" + AuthoritiesConstants.ADMIN + "')")
  @PostMapping
  public ResponseEntity<CategoryResponseDto> create(@Valid @RequestBody CategoryCreateDto dto) throws URISyntaxException {
    LOG.debug("REST request to save Category : {}", dto);

    CategoryResponseDto responseDto = categoryService.save(dto);

    return ResponseEntity.created(
      new URI("/api/categories/" + responseDto.id())
    )
      .headers(
        HeaderUtil.createEntityCreationAlert(
          applicationName,
          false,
          ENTITY_NAME,
          responseDto.id().toString()
        )
      )
      .body(responseDto);
  }

  /**
   * {@code GET  /categories} : get all the Categories.
   *
   * @param pageable the pagination information.
   * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Categories in body.
   */
  @GetMapping
  public ResponseEntity<List<CategoryResponseDto>> getAllCategories(
    @ParameterObject Pageable pageable,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant updatedSince
  ) {
    LOG.debug("REST request to get a page of Categories");

    Page<CategoryResponseDto> page = categoryService.findAll(pageable, updatedSince);

    HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(
      ServletUriComponentsBuilder.fromCurrentRequest(),
      page
    );

    return ResponseEntity.ok().headers(headers).body(page.getContent());
  }

  /**
   * {@code GET  /categories/:id} : get the "id" category.
   *
   * @param id the id of the categoryDTO to retrieve.
   * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the categoryDTO, or with status {@code 404 (Not Found)}.
   */
  @GetMapping("/{id}")
  public ResponseEntity<CategoryResponseDto> getCategory(@PathVariable("id") Long id) {
    LOG.debug("REST request to get Category : {}", id);

    CategoryResponseDto categoryDTO = categoryService.findOne(id);
    return ResponseEntity.ok().body(categoryDTO);
    //return ResponseUtil.wrapOrNotFound(categoryDTO);
  }

  @PreAuthorize("hasAuthority('" + AuthoritiesConstants.ADMIN + "')")
  @PatchMapping("/{id}/active")
  public ResponseEntity<CategoryResponseDto> updateActive(
    @PathVariable Long id,
    @Valid @RequestBody ActiveUpdateDto dto
  ) {
    return ResponseEntity.ok(categoryService.updateActive(id, dto.active()));
  }
}
