package com.smartrent.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration for cache management.
 * Provides a NoOp cache manager for tests to avoid Redis dependencies.
 */
@TestConfiguration
public class TestCacheConfig {

    @Bean
    @Primary
    public CacheManager cacheManager() {
        return new NoOpCacheManager();
    }
}
