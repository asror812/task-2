package com.devops.paylite.mapper;

import com.devops.paylite.domain.Payment;
import com.devops.paylite.service.dto.PaymentDTO;
import org.mapstruct.Mapper;

/**
 * Mapper for the entity {@link Payment} and its DTO {@link PaymentDTO}.
 */
@Mapper(componentModel = "spring")
public interface PaymentMapper {
    PaymentDTO toDto(Payment s);
}
