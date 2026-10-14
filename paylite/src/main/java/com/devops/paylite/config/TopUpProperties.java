package com.devops.paylite.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.validation.annotation.Validated;

@RefreshScope
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "top-up", ignoreUnknownFields = false)
public class TopUpProperties {

    @NotNull(message = "Maximum top-up amount must not be null")
    @Min(value = 1, message = "Maximum top-up amount must be greater than zero")
    private Long maxAmount = 100_000_000L;
}
