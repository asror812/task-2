package com.devops.paylite.service.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentCheckResponseDto {
    private boolean valid;
    private String recipientName;
    private String checkId;
    private List<String> errors;
}
