package com.devops.paylite.mapper;

import com.devops.paylite.service.dto.paynet.PaynetPayRequestDto;
import com.devops.paylite.service.dto.payment.PaymentCreateRequestDto;
import org.springframework.stereotype.Component;

@Component
public class PaymentPayMapper {

    public PaynetPayRequestDto toPaynetPayRequest(PaymentCreateRequestDto dto) {
        return new PaynetPayRequestDto(dto.getCheckId(), dto.getIdempotencyKey(), dto.getFields(), dto.getAmount());
    }
}
