package com.devops.paylite.config;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.validation.annotation.Validated;

@RefreshScope
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "payment", ignoreUnknownFields = false)
public class PaymentProperties {

    @NotNull(message = "Commission percent must not be null")
    @DecimalMin(value = "0.0", message = "Commission percent must be greater than or equal to 0.0")
    @DecimalMax(value = "100.0", message = "Commission percent must be less than or equal to 100.0")
    private BigDecimal commissionPercent;


    @NotNull(message = "Minimum amount must not be null")
    @Min(value = 1, message = "Minimum amount must be greater than zero")
    private Long minAmount = 100L;

    @NotNull(message = "Maximum amount must not be null")
    @Min(value = 1, message = "Maximum amount must be greater than zero")
    private Long maxAmount = 100_000_000L;

    @Min(value = 1, message = "Maximum attempts must be greater than zero")
    private int maxAttempts = 4;

    @Min(value = 1, message = "Outbox batch size must be greater than zero")
    private int outboxBatchSize = 50;

    @Min(value = 1, message = "Outbox publish delay must be greater than zero")
    private long outboxPublishDelayMs = 1_000L;

    @Min(value = 1, message = "Reconciliation delay must be greater than zero")
    private long reconciliationDelayMs = 900_000L;

    @Min(value = 1, message = "Reconciliation stale threshold must be greater than zero")
    private long reconciliationStaleAfterMs = 900_000L;

    @AssertTrue(message = "Minimum amount must not exceed maximum amount")
    public boolean isAmountRangeValid() {
        return minAmount == null || maxAmount == null || minAmount <= maxAmount;
    }
}
