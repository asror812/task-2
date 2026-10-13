package com.devops.paylite.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "top_up_ledger",
    uniqueConstraints = @UniqueConstraint(name = "ux_top_up_ledger__idempotency_key", columnNames = "idempotency_key")
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class TopUpLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_login", nullable = false, length = 100)
    private String adminLogin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "balance_before", nullable = false)
    private Long balanceBefore;

    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
