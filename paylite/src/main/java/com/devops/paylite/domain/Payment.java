package com.devops.paylite.domain;

import com.devops.paylite.domain.enumeration.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "payment")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "payment_reference", nullable = false, columnDefinition = "text")
    private String paymentReference;

    @NotNull
    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "commission_amount")
    private Long commissionAmount;

    @Column(name = "total_amount")
    private Long totalAmount;

    @Column(name = "check_id", unique = true)
    private String checkId;

    @Column(name = "recipient_id")
    private Long recipientId;

    @Column(name = "paynet_payment_id")
    private String paynetPaymentId;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "created_date")
    private Instant createdDate;

    @Column(name = "updated_date")
    private Instant updatedDate;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Payment)) {
            return false;
        }
        return getId() != null && getId().equals(((Payment) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return (
            "Payment{" +
            "id=" +
            getId() +
            ", amount=" +
            getAmount() +
            ", commissionAmount=" +
            getCommissionAmount() +
            ", totalAmount=" +
            getTotalAmount() +
            ", status='" +
            getStatus() +
            "'" +
            ", createdDate='" +
            getCreatedDate() +
            "'" +
            "}"
        );
    }
}
