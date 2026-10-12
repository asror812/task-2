package com.devops.paylite.service.dto.payment;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentCreateRequestDto {

    @NotNull(message = "PaynetRecipient ID is required")
    private Long recipientId;

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    @NotBlank(message = "Check ID is required")
    private String checkId;

    @NotEmpty(message = "payment fields are required")
    private Map<String, Object> fields;

    @NotBlank
    @Size(max = 255)
    private String account;

    @Positive
    @NotNull
    private Long amount;
}
