package com.devops.paynet.service;

import com.devops.paynet.domain.Category;
import com.devops.paynet.repository.CategoryRepository;
import com.devops.paynet.dto.CategoryCreateDto;
import com.devops.paynet.dto.CategoryResponseDto;
import com.devops.paynet.mapper.CategoryMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

  private static final Logger LOG = LoggerFactory.getLogger(CategoryService.class);

  private final CategoryRepository categoryRepository;
  private final CategoryMapper categoryMapper;

  public CategoryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
    this.categoryRepository = categoryRepository;
    this.categoryMapper = categoryMapper;
  }

  /**
   * Save a category.
   *
   * @param categoryDTO the entity to save.
   * @return the persisted entity.
   */
  public CategoryResponseDto save(CategoryCreateDto categoryDTO) {
    LOG.debug("Request to save Category : {}", categoryDTO);

    Category category = categoryMapper.toEntity(categoryDTO);
    category = categoryRepository.save(category);

    return categoryMapper.toResponseDto(category);
  }

  /**
   * Get all the categories.
   *
   * @param pageable the pagination information.
   * @return the list of entities.
   */
  @Transactional(readOnly = true)
  public Page<CategoryResponseDto> findAll(Pageable pageable) {
    LOG.debug("Request to get all Categories");
    return categoryRepository.findAll(pageable).map(categoryMapper::toResponseDto);
  }

  /**
   * Get one category by id.
   *
   * @param id the id of the entity.
   * @return the entity.
   */
  public CategoryResponseDto findOne(Long id) {
    LOG.debug("Request to get Category : {}", id);
    return categoryRepository.findById(id).map(categoryMapper::toResponseDto)
            .orElseThrow(() -> new RuntimeException("Category not found with id " + id));
  }
}
