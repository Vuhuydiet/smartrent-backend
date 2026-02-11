package com.smartrent.config.otp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for OTP module
 */
@Configuration
public class OtpConfig {

    /**
     * WebClient for HTTP requests to external providers
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}

