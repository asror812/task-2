package com.devops.paynet.config;

import com.devops.paynet.aop.logging.LoggingAspect;
import org.springframework.context.annotation.*;
import tech.jhipster.config.JHipsterConstants;

@Configuration
@EnableAspectJAutoProxy
public class LoggingAspectConfiguration {

    @Bean
    @Profile(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT)
    public LoggingAspect loggingAspect() {
        return new LoggingAspect();
    }
}
