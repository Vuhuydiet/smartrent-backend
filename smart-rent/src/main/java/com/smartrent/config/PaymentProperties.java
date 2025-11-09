package com.smartrent.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "payment")
@Getter
@Setter
public class PaymentProperties {
    /**
     * Frontend redirect URL after payment completion
     * Default: http://localhost:3000/payment/result
     */
    private String redirectUrl;
}

