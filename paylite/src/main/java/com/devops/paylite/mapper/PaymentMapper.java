package com.devops.paylite.mapper;

import com.devops.paylite.domain.Agent;
import com.devops.paylite.domain.Payment;
import com.devops.paylite.service.dto.AgentDTO;
import com.devops.paylite.service.dto.PaymentDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Payment} and its DTO {@link PaymentDTO}.
 */
@Mapper(componentModel = "spring")
public interface PaymentMapper extends EntityMapper<PaymentDTO, Payment> {
    @Mapping(target = "agent", source = "agent", qualifiedByName = "agentId")
    PaymentDTO toDto(Payment s);

    @Mapping(target = "requestPayload", ignore = true)
    Payment toEntity(PaymentDTO dto);

    @Mapping(target = "requestPayload", ignore = true)
    void partialUpdate(@MappingTarget Payment entity, PaymentDTO dto);

    @Named("agentId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    AgentDTO toDtoAgentId(Agent agent);
}
