package com.devops.paynet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PayRequestDto {

    @NotBlank(message = "Check ID is required")
    private String checkId;

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

}
