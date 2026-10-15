package com.devops.paylite.service.dto.load;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaynetCatalogEntryLoadDto {

    private PaynetCategoryLoadDto category;
    private PaynetRecipientLoadDto recipient;
    private List<PaynetRecipientFieldLoadDto> fields;
}
