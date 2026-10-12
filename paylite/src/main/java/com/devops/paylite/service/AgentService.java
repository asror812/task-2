package com.devops.paylite.service;

import com.devops.paylite.domain.Agent;
import com.devops.paylite.repository.AgentRepository;
import com.devops.paylite.security.SecurityUtils;
import com.devops.paylite.service.dto.AgentDTO;
import com.devops.paylite.service.dto.BalanceResponseDto;
import com.devops.paylite.service.dto.TopUpRequestDto;
import com.devops.paylite.mapper.AgentMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AgentService {

    private static final Logger LOG = LoggerFactory.getLogger(AgentService.class);

    private final AgentRepository agentRepository;
    private final AgentMapper agentMapper;

    @Transactional(readOnly = true)
    public List<AgentDTO> getAll() {
        return agentRepository.findAllByOrderByLoginAsc().stream().map(agentMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public BalanceResponseDto getOwnBalance() {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current user login not found"));

        Agent agent = agentRepository
            .findByLogin(login)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found"));

        return new BalanceResponseDto(agent.getBalance());
    }

    @Transactional
    public void topUp(Long id, TopUpRequestDto dto) {
        LOG.debug("Request to top up Agent: {}", id);

        Agent agent = agentRepository
            .findByIdForUpdate(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found"));

        try {
            agent.setBalance(Math.addExact(agent.getBalance(), dto.amount()));
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Top-up amount is too large", exception);
        }
        agentRepository.save(agent);
    }
}
