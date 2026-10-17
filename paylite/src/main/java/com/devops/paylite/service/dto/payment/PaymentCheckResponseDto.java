package com.devops.paylite.service.dto.payment;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentCheckResponseDto {

    private boolean valid;
    private String recipientName;
    private String checkId;
    private String paymentReference;
    private List<String> errors;
}
