package com.devops.paylite.mapper;

import com.devops.paylite.service.dto.paynet.PaynetCheckRequestDto;
import com.devops.paylite.service.dto.paynet.PaynetCheckResponseDto;
import com.devops.paylite.service.dto.payment.PaymentCheckRequestDto;
import com.devops.paylite.service.dto.payment.PaymentCheckResponseDto;
import org.springframework.stereotype.Component;

@Component
public class PaymentCheckMapper {

    public PaynetCheckRequestDto toPaynetCheckRequest(PaymentCheckRequestDto dto) {
        return new PaynetCheckRequestDto(dto.getFields());
    }

    public PaymentCheckResponseDto toPaymentCheckResponse(PaynetCheckResponseDto dto) {
        return new PaymentCheckResponseDto(
            dto.isValid(),
            dto.getRecipientName(),
            dto.getCheckId(),
            dto.getErrors()
        );
    }
}
