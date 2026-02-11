package com.smartrent.config.cache;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("spring.cache.redis")
public class ExtendedCacheProperties {

  // cacheName - expireTime
  Map<String, Duration> expires = new HashMap<>();
}
