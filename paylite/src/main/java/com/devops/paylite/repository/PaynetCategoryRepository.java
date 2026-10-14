package com.devops.paylite.repository;

import com.devops.paylite.domain.PaynetCategory;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaynetCategoryRepository extends JpaRepository<PaynetCategory, Long> {
    List<PaynetCategory> findAllByPaynetIdIn(Collection<Long> paynetIds);

    @Modifying(flushAutomatically = true)
    @Query(
        "update PaynetCategory category " +
        "set category.active = false, category.lastSyncedAt = :syncedAt " +
        "where category.lastSyncedAt is null or category.lastSyncedAt <> :syncedAt"
    )
    int deactivateNotSyncedAt(@Param("syncedAt") Instant syncedAt);
}
