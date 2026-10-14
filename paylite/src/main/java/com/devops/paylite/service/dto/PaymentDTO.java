package com.devops.paylite.service.dto;

import com.devops.paylite.domain.enumeration.PaymentStatus;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PaymentDTO implements Serializable {

    private Long id;

    @NotNull
    private String account;

    @NotNull
    private Long amount;

    private Long commissionAmount;

    private Long totalAmount;

    private String checkId;

    private String paynetPaymentId;

    private String idempotencyKey;

    @NotNull
    private PaymentStatus status;

    private Instant createdDate;

    private AgentDTO agent;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PaymentDTO paymentDTO)) {
            return false;
        }

        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, paymentDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PaymentDTO{" +
            "id=" + getId() +
            ", amount=" + getAmount() +
            ", commissionAmount=" + getCommissionAmount() +
            ", totalAmount=" + getTotalAmount() +
            ", status='" + getStatus() + "'" +
            ", createdDate='" + getCreatedDate() + "'" +
            ", agent=" + getAgent() +
            "}";
    }
}
