package com.devops.paylite.repository;

import com.devops.paylite.domain.TopUpLedger;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TopUpLedgerRepository extends JpaRepository<TopUpLedger, Long> {
    Optional<TopUpLedger> findByIdempotencyKey(String idempotencyKey);
}
