package com.devops.paylite.service.dto.paynet;

import java.util.List;

public record PaynetPaymentStatusResponseDto(
    PaynetPaymentLookupStatus status,
    String paymentId,
    List<String> errors
) {}
