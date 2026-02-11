package com.smartrent.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
public class ConfigTestController {

    @Value("${smartrent.ai.verification.timeout-seconds:15}")
    private int timeoutSeconds;

    @Value("${smartrent.ai.verification.url:default-url}")
    private String aiVerificationUrl;

    @GetMapping("/debug/config")
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("timeout-seconds", timeoutSeconds);
        config.put("ai-verification-url", aiVerificationUrl);
        config.put("active-profiles", System.getProperty("spring.profiles.active"));
        log.info("Config debug - timeout: {}, url: {}", timeoutSeconds, aiVerificationUrl);
        return config;
    }
}