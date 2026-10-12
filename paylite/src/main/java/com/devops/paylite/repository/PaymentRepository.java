package com.devops.paylite.repository;

import com.devops.paylite.domain.Payment;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Payment entity.
 */
@SuppressWarnings("unused")
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Page<Payment> findAllByAgent_Login(String account, Pageable pageable);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
