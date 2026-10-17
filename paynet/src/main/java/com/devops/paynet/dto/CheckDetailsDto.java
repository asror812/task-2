package com.devops.paynet.dto;

public record CheckDetailsDto(
    String checkId,
    Long recipientId,
    Long amount,
    String paymentReference
) {}
