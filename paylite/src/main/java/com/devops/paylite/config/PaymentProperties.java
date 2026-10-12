package com.devops.paylite.config;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "payment", ignoreUnknownFields = false)
public class PaymentProperties {

    private BigDecimal commissionPercent;
}
