package com.devops.paylite.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;

@Configuration
public class OAuth2Configuration {

    @Bean
    @Primary
    public OAuth2AuthorizedClientManager authorizedClientManager(
        ClientRegistrationRepository clientRegistrationRepository,
        OAuth2AuthorizedClientRepository authorizedClientRepository
    ) {
        DefaultOAuth2AuthorizedClientManager authorizedClientManager = new DefaultOAuth2AuthorizedClientManager(
            clientRegistrationRepository,
            authorizedClientRepository
        );

        authorizedClientManager.setAuthorizedClientProvider(
            OAuth2AuthorizedClientProviderBuilder.builder()
                .authorizationCode()
                .refreshToken(builder -> builder.clockSkew(Duration.ofMinutes(1)))
                .clientCredentials()
                .build()
        );

        return authorizedClientManager;
    }

    @Bean("paynetAuthorizedClientManager")
    public OAuth2AuthorizedClientManager paynetAuthorizedClientManager(
        ClientRegistrationRepository clientRegistrationRepository,
        OAuth2AuthorizedClientService authorizedClientService
    ) {
        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
            new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientService);

        authorizedClientManager.setAuthorizedClientProvider(
            OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build()
        );

        return authorizedClientManager;
    }
}
