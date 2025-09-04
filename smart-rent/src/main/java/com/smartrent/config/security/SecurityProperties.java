package com.smartrent.config.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "application.authorize-ignored")
public class SecurityProperties {
    
    private Methods methods = new Methods();
    
    @Data
    public static class Methods {
        private List<String> post;
        private List<String> get;
    }
}
