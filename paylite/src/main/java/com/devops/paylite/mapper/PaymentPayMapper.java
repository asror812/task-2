package com.devops.paylite.mapper;

import com.devops.paylite.domain.Payment;
import com.devops.paylite.service.dto.paynet.PaynetPayRequestDto;
import org.springframework.stereotype.Component;

@Component
public class PaymentPayMapper {

    public PaynetPayRequestDto toPaynetPayRequest(Payment payment) {
        return new PaynetPayRequestDto(payment.getCheckId(), payment.getIdempotencyKey());
    }
}
