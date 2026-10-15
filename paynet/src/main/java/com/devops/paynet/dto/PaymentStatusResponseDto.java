package com.devops.paynet.dto;

import java.util.List;

public record PaymentStatusResponseDto(
    PaymentLookupStatus status,
    String paymentId,
    List<String> errors
) {}
