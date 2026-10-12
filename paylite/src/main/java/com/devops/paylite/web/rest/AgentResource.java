package com.devops.paylite.web.rest;

import com.devops.paylite.service.AgentService;
import com.devops.paylite.service.dto.AgentDTO;
import com.devops.paylite.service.dto.BalanceResponseDto;
import com.devops.paylite.service.dto.TopUpRequestDto;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing {@link com.devops.paylite.domain.Agent}.
 */
@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentResource {
    private static final Logger LOG = LoggerFactory.getLogger(AgentResource.class);
    private final AgentService agentService;

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping
    public ResponseEntity<List<AgentDTO>> getAll() {
        LOG.debug("REST request to get all Agents for administration");
        return ResponseEntity.ok(agentService.getAll());
    }

    @PreAuthorize("hasAuthority('ROLE_AGENT')")
    @GetMapping("/me/balance")
    public ResponseEntity<BalanceResponseDto> getOwnBalance() {
        LOG.debug("REST request to get Own Balance");

        BalanceResponseDto dto = agentService.getOwnBalance();
        return ResponseEntity.ok().body(dto);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/{id}/topup")
    public ResponseEntity<Void> topUp(@PathVariable Long id, @Valid @RequestBody TopUpRequestDto dto) {
        LOG.debug("REST request to top up Agent: {}", id);

        agentService.topUp(id, dto);

        return ResponseEntity.ok().build();
    }
}
