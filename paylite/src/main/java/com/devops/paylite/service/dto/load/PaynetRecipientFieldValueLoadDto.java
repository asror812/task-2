package com.devops.paylite.service.dto.load;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaynetRecipientFieldValueLoadDto {

    private Long id;
    private String value;
    private String label;
    private Boolean active;
    private Integer sortOrder;
    private Long parentId;
}
