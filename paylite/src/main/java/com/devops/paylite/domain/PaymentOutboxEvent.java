package com.devops.paylite.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "payment_outbox_event")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "publish_attempts", nullable = false)
    @Builder.Default
    private Integer publishAttempts = 0;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    public static PaymentOutboxEvent paymentCreated(Long paymentId) {
        return PaymentOutboxEvent.builder()
            .paymentId(paymentId)
            .eventType("PAYMENT_CREATED")
            .createdAt(Instant.now())
            .build();
    }

    public static PaymentOutboxEvent paymentRecoveryRequested(Long paymentId) {
        return PaymentOutboxEvent.builder()
            .paymentId(paymentId)
            .eventType("PAYMENT_RECOVERY_REQUESTED")
            .createdAt(Instant.now())
            .build();
    }
}
