package com.devops.paylite.service.dto.payment;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentCheckRequestDto {

    @NotNull(message = "PaynetRecipient ID is required")
    private Long recipientId;

    @NotEmpty(message = "payment fields are required")
    private Map<String, Object> fields;
}
