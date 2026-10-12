package com.devops.paynet.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("api-docs")
@OpenAPIDefinition(security = @SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH))
@SecurityScheme(
    name = OpenApiConfiguration.BEARER_AUTH,
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
public class OpenApiConfiguration {

    public static final String BEARER_AUTH = "bearerAuth";
}
