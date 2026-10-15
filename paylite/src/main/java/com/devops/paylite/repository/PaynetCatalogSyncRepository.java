package com.devops.paylite.repository;

import com.devops.paylite.domain.PaynetCatalogSync;
import java.util.Optional;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface PaynetCatalogSyncRepository extends JpaRepository<PaynetCatalogSync, Long> {
    Optional<PaynetCatalogSync> findTopByOrderByStartedAtDesc();

    @Modifying
    @Transactional
    @Query(
        "update PaynetCatalogSync sync " +
        "set " +
            "sync.status = com.devops.paylite.domain.enumeration.SyncStatus.FAILED, " +
            "sync.finishedAt = :now, " +
            "sync.errorMessage = 'Synchronization lease expired' " +
        "where sync.status = com.devops.paylite.domain.enumeration.SyncStatus.RUNNING and sync.startedAt < :cutoff"
    )
    int failStaleRunning(Instant cutoff, Instant now);
}
