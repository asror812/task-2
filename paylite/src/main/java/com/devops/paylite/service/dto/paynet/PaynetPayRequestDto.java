package com.devops.paylite.service.dto.paynet;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaynetPayRequestDto {

    private String checkId;
    private String idempotencyKey;
    private Map<String, Object> fields;
    private Long amount;
}
