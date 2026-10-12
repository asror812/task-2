package com.devops.paylite.web.rest;

import com.devops.paylite.service.PaynetCatalogSyncService;
import com.devops.paylite.service.dto.PaynetCatalogSyncResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/paynet-catalog")
@RequiredArgsConstructor
public class PaynetCatalogSyncResource {

    private final PaynetCatalogSyncService paynetCatalogSyncService;

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/sync")
    public ResponseEntity<PaynetCatalogSyncResponseDto> sync() {
        return ResponseEntity.ok(paynetCatalogSyncService.sync());
    }
}
