package com.devops.paylite.repository;

import com.devops.paylite.domain.PaynetRecipientFieldValue;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaynetRecipientFieldValueRepository extends JpaRepository<PaynetRecipientFieldValue, Long> {
    List<PaynetRecipientFieldValue> findAllByPaynetIdIn(Collection<Long> paynetIds);

    @Modifying(flushAutomatically = true)
    @Query(
        "update PaynetRecipientFieldValue fieldValue " +
        "set fieldValue.active = false, fieldValue.lastSyncedAt = :syncedAt " +
        "where fieldValue.lastSyncedAt is null or fieldValue.lastSyncedAt <> :syncedAt"
    )
    int deactivateNotSyncedAt(@Param("syncedAt") Instant syncedAt);
}
