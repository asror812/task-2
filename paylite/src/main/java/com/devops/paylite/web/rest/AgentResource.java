package com.devops.paylite.web.rest;

import com.devops.paylite.service.AgentService;
import com.devops.paylite.service.dto.AgentDTO;
import com.devops.paylite.service.dto.BalanceResponseDto;
import com.devops.paylite.service.dto.TopUpRequestDto;
import com.devops.paylite.service.dto.TopUpResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    public ResponseEntity<Page<AgentDTO>> getAll(@ParameterObject Pageable pageable) {
        LOG.debug("REST request to get a page of Agents for administration");
        return ResponseEntity.ok(agentService.getAll(pageable));
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
    public ResponseEntity<TopUpResponseDto> topUp(@PathVariable Long id, @Valid @RequestBody TopUpRequestDto dto) {
        LOG.debug("REST request to top up Agent: {}", id);

        return ResponseEntity.ok(agentService.topUp(id, dto));
    }
}
