package com.devops.paylite.service.dto.paynet;

public record PaynetCheckDetailsDto(String checkId, Long recipientId, Long amount, String paymentReference) {}
