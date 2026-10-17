package com.devops.paylite.service.dto.paynet;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaynetPayResponseDto {

    private boolean paid;
    private String paymentId;
    private String checkId;
    private Long recipientId;
    private String recipientName;
    private List<String> errors;
}
