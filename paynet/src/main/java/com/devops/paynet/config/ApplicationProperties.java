package com.devops.paynet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

    private final Liquibase liquibase = new Liquibase();

    public Liquibase getLiquibase() {
        return liquibase;
    }

    public static class Liquibase {
        private Boolean asyncStart = true;

        public Boolean getAsyncStart() {
            return asyncStart;
        }

        public void setAsyncStart(Boolean asyncStart) {
            this.asyncStart = asyncStart;
        }
    }
    // jhipster-needle-application-properties-property-class
}
