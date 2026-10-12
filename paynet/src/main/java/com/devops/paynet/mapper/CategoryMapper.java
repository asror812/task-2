package com.devops.paynet.mapper;

import com.devops.paynet.domain.Category;
import com.devops.paynet.dto.CategoryCreateDto;
import com.devops.paynet.dto.CategoryResponseDto;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryResponseDto toResponseDto(Category category);
    Category toEntity(CategoryCreateDto categoryDTO);
}
