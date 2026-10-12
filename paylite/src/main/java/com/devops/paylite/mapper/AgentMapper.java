package com.devops.paylite.mapper;

import com.devops.paylite.domain.Agent;
import com.devops.paylite.service.dto.AgentDTO;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface AgentMapper extends EntityMapper<AgentDTO, Agent> {}
