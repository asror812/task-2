package com.devops.paylite.service.dto.load;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PaynetCategoryLoadDto {

    private Long id;
    private String name;
    private Boolean active;

    private Integer sortOrder;
}
