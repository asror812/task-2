package com.devops.paylite.service.dto.load;

import com.devops.paylite.domain.enumeration.RecipientFieldType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaynetRecipientFieldLoadDto {

    private Long id;
    private String name;
    private String label;
    private String validationRegex;
    private Boolean active;
    private Boolean required;
    private RecipientFieldType type;
    private Integer sortOrder;
    private Long recipientId;
    private List<PaynetRecipientFieldValueLoadDto> values;
}
