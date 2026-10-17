package com.devops.paylite.repository;

import com.devops.paylite.domain.Payment;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Payment entity.
 */
@SuppressWarnings("unused")
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Page<Payment> findAllByAgent_Login(String login, Pageable pageable);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByCheckId(String checkId);

    @EntityGraph(attributePaths = "agent")
    Optional<Payment> findOneWithAgentById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from Payment payment join fetch payment.agent where payment.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        "select payment from Payment payment where payment.status in :statuses " +
        "and payment.updatedDate < :updatedBefore order by payment.updatedDate asc"
    )
    List<Payment> findStaleForUpdate(
        @Param("statuses") Collection<com.devops.paylite.domain.enumeration.PaymentStatus> statuses,
        @Param("updatedBefore") Instant updatedBefore,
        Pageable pageable
    );
}
