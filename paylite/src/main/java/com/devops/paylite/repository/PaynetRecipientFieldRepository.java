package com.devops.paylite.repository;

import com.devops.paylite.domain.PaynetRecipientField;
import com.devops.paylite.domain.enumeration.RecipientFieldMethod;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaynetRecipientFieldRepository extends JpaRepository<PaynetRecipientField, Long> {
    List<PaynetRecipientField> findAllByPaynetIdIn(Collection<Long> paynetIds);

    List<PaynetRecipientField> findAllByRecipientPaynetIdAndActiveTrueAndMethodOrderBySortOrderAscIdAsc(
        Long recipientPaynetId,
        RecipientFieldMethod method
    );

    @Modifying(flushAutomatically = true)
    @Query(
        "update PaynetRecipientField field " +
        "set field.active = false, field.lastSyncedAt = :syncedAt " +
        "where field.lastSyncedAt is null or field.lastSyncedAt <> :syncedAt"
    )
    int deactivateNotSyncedAt(@Param("syncedAt") Instant syncedAt);
}
