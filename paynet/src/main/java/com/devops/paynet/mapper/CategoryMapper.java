package com.devops.paynet.mapper;

import com.devops.paynet.domain.Category;
import com.devops.paynet.dto.CategoryCreateDto;
import com.devops.paynet.dto.CategoryResponseDto;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    default CategoryResponseDto toResponseDto(Category category) {
        if (category == null) {
            return null;
        }
        return new CategoryResponseDto(
            category.getId(),
            category.getName(),
            category.getActive(),
            category.getSortOrder(),
            category.getLastModifiedDate()
        );
    }

    Category toEntity(CategoryCreateDto categoryDTO);
}
