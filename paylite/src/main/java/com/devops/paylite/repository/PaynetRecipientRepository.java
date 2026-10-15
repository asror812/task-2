package com.devops.paylite.repository;

import com.devops.paylite.domain.PaynetRecipient;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaynetRecipientRepository extends JpaRepository<PaynetRecipient, Long> {
    List<PaynetRecipient> findAllByPaynetIdIn(Collection<Long> paynetIds);

    @EntityGraph(attributePaths = {"category"})
    Page<PaynetRecipient> findAllByActiveTrueOrderByNameAsc(Pageable pageable);

    @Modifying(flushAutomatically = true)
    @Query(
        "update PaynetRecipient recipient " +
        "set recipient.active = false, recipient.lastSyncedAt = :syncedAt " +
        "where recipient.lastSyncedAt is null or recipient.lastSyncedAt <> :syncedAt"
    )
    int deactivateNotSyncedAt(@Param("syncedAt") Instant syncedAt);
}
