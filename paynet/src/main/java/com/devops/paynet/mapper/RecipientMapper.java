package com.devops.paynet.mapper;


import com.devops.paynet.domain.Category;
import com.devops.paynet.domain.Recipient;
import com.devops.paynet.dto.RecipientCreateDto;
import com.devops.paynet.dto.RecipientResponseDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RecipientMapper {
  default Recipient toEntity(RecipientCreateDto recipientDTO) {
    if (recipientDTO == null) {
      return null;
    }

    Recipient recipient = new Recipient();
    recipient.setName(recipientDTO.name());
    recipient.setCode(recipientDTO.code());
    recipient.setActive(recipientDTO.active());

    if (recipientDTO.categoryId() != null) {
      Category category = new Category();
      category.setId(recipientDTO.categoryId());
      recipient.setCategory(category);
    }

    return recipient;
  }

  default RecipientResponseDto toResponseDto(Recipient recipient) {
    if (recipient == null) {
      return null;
    }

    Category category = recipient.getCategory();
    return new RecipientResponseDto(
      recipient.getId(),
      recipient.getName(),
      recipient.getCode(),
      recipient.getActive(),
      category != null ? category.getName() : null,
      category != null ? category.getId() : null
    );
  }
}
