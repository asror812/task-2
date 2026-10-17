package com.devops.paynet.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "payment")
public class PaymentProperties {

    @NotNull
    private Duration checkTtl = Duration.ofMinutes(15);

    @NotNull
    private Duration processingLockTtl = Duration.ofSeconds(60);

    @NotNull
    private Duration idempotencyTtl = Duration.ofDays(7);
}
