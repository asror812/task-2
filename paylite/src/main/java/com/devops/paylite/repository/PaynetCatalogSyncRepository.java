package com.devops.paylite.repository;

import com.devops.paylite.domain.PaynetCatalogSync;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaynetCatalogSyncRepository extends JpaRepository<PaynetCatalogSync, Long> {
    Optional<PaynetCatalogSync> findTopByOrderByStartedAtDesc();
}
