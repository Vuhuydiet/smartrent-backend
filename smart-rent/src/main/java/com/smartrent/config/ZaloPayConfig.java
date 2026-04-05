package com.smartrent.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "zalopay")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ZaloPayConfig {
    String appId;
    String key1;
    String key2;
    String createOrderUrl;
    String queryOrderUrl;
    String returnUrl;
    String ipnUrl;
    Integer timeoutMinutes = 15;
}
