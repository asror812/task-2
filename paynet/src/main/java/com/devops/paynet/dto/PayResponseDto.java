package com.devops.paynet.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PayResponseDto {

    private boolean paid;
    private String paymentId;
    private String checkId;
    private Long recipientId;
    private String recipientName;
    private List<String> errors;

    public PayResponseDto(boolean paid, String paymentId, Long recipientId, String recipientName, List<String> errors) {
        this.paid = paid;
        this.paymentId = paymentId;
        this.recipientId = recipientId;
        this.recipientName = recipientName;
        this.errors = errors;
    }

}
