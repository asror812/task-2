package com.devops.paylite.service;

import com.devops.paylite.config.TopUpProperties;
import com.devops.paylite.domain.Agent;
import com.devops.paylite.domain.TopUpLedger;
import com.devops.paylite.repository.AgentRepository;
import com.devops.paylite.repository.TopUpLedgerRepository;
import com.devops.paylite.security.SecurityUtils;
import com.devops.paylite.service.dto.AgentDTO;
import com.devops.paylite.service.dto.BalanceResponseDto;
import com.devops.paylite.service.dto.TopUpRequestDto;
import com.devops.paylite.service.dto.TopUpResponseDto;
import com.devops.paylite.mapper.AgentMapper;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AgentService {

    private static final Logger LOG = LoggerFactory.getLogger(AgentService.class);

    private final AgentRepository agentRepository;
    private final TopUpLedgerRepository topUpLedgerRepository;
    private final AgentMapper agentMapper;
    private final TopUpProperties topUpProperties;

    @Transactional(readOnly = true)
    public Page<AgentDTO> getAll(Pageable pageable) {
        return agentRepository.findAllByOrderByLoginAsc(pageable).map(agentMapper::toDto);
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
    public TopUpResponseDto topUp(Long id, TopUpRequestDto dto) {
        LOG.debug("Request to top up Agent: {}", id);

        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current user login not found"));

        Agent agent = agentRepository
            .findByIdForUpdate(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found"));

        TopUpLedger existingLedger = topUpLedgerRepository.findByIdempotencyKey(dto.idempotencyKey()).orElse(null);
        if (existingLedger != null) {
            if (
                !login.equals(existingLedger.getAdminLogin()) ||
                !id.equals(existingLedger.getAgent().getId()) ||
                !dto.amount().equals(existingLedger.getAmount())
            ) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key already used");
            }
            return toTopUpResponse(existingLedger);
        }

        validateTopUpAmount(dto.amount());

        Long balanceBefore = agent.getBalance();
        Long balanceAfter;

        try {
            balanceAfter = Math.addExact(agent.getBalance(), dto.amount());
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Top-up amount is too large", exception);
        }
        agent.setBalance(balanceAfter);

        TopUpLedger topUpLedger = TopUpLedger.builder()
            .adminLogin(login)
            .agent(agent)
            .amount(dto.amount())
            .balanceBefore(balanceBefore)
            .balanceAfter(balanceAfter)
            .idempotencyKey(dto.idempotencyKey())
            .createdAt(Instant.now())
            .build();

        agentRepository.save(agent);
        return toTopUpResponse(topUpLedgerRepository.save(topUpLedger));
    }

    private void validateTopUpAmount(Long amount) {
        if (amount > topUpProperties.getMaxAmount()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Top-up amount exceeds the maximum limit");
        }
    }

    private TopUpResponseDto toTopUpResponse(TopUpLedger ledger) {
        return new TopUpResponseDto(
            ledger.getId(),
            ledger.getAgent().getId(),
            ledger.getAmount(),
            ledger.getBalanceBefore(),
            ledger.getBalanceAfter(),
            ledger.getCreatedAt()
        );
    }
}
