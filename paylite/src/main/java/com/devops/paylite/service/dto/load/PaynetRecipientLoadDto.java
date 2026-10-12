package com.devops.paylite.service.dto.load;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaynetRecipientLoadDto {
    private Long id;
    private String name;
    private String code;
    private Boolean active;
    private Long categoryId;
}
