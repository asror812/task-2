package com.devops.paylite.repository;

import com.devops.paylite.domain.Payment;
import jakarta.validation.constraints.NotNull;
import jakarta.persistence.LockModeType;
import java.time.Instant;
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
    Page<Payment> findAllByAgent_Login(String account, Pageable pageable);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    @EntityGraph(attributePaths = "agent")
    Optional<Payment> findOneWithAgentById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from Payment payment join fetch payment.agent where payment.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") Long id);

    List<Payment> findTop50ByStatusAndUpdatedDateBeforeOrderByUpdatedDateAsc(
        com.devops.paylite.domain.enumeration.PaymentStatus status,
        Instant updatedBefore
    );
}
