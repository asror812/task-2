package com.devops.paylite.repository;

import com.devops.paylite.domain.PaymentOutboxEvent;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentOutboxEventRepository extends JpaRepository<PaymentOutboxEvent, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        "select event from PaymentOutboxEvent event " +
        "where event.publishedAt is null order by event.createdAt asc"
    )
    List<PaymentOutboxEvent> findUnpublishedForUpdate(Pageable pageable);
}
