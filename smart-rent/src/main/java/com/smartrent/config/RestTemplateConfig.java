package com.smartrent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@Slf4j
public class RestTemplateConfig {

    @Value("${smartrent.ai.verification.timeout-seconds:15}")
    private int timeoutSeconds;

    @Bean
    public RestTemplate restTemplate() {
        log.info("Configuring RestTemplate with timeout: {} seconds (from property: smartrent.ai.verification.timeout-seconds)", timeoutSeconds);
        
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        
        // Convert seconds to milliseconds
        int timeoutMillis = timeoutSeconds * 1000;
        
        factory.setConnectTimeout(timeoutMillis);
        factory.setReadTimeout(timeoutMillis);
        
        return new RestTemplate(factory);
    }
}