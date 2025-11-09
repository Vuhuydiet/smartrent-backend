package com.smartrent.service.otp.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based implementation of OTP store
 * Uses Redis SET with EX (expiration) and NX (if not exists) for atomic operations
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "otp.store.type", havingValue = "redis", matchIfMissing = true)
public class RedisOtpStore implements OtpStore {

    private static final String KEY_PREFIX = "otp:";
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisOtpStore(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public boolean store(OtpData otpData, int ttlSeconds) {
        try {
            String key = buildKey(otpData.getPhone(), otpData.getRequestId());
            String value = objectMapper.writeValueAsString(otpData);
            
            // Use SET with NX (only set if not exists) and EX (expiration)
            Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(key, value, ttlSeconds, TimeUnit.SECONDS);
            
            log.debug("Stored OTP for key: {}, result: {}", key, result);
            return Boolean.TRUE.equals(result);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OTP data", e);
            return false;
        } catch (Exception e) {
            log.error("Failed to store OTP in Redis", e);
            return false;
        }
    }

    @Override
    public Optional<OtpData> retrieve(String phone, String requestId) {
        try {
            String key = buildKey(phone, requestId);
            String value = redisTemplate.opsForValue().get(key);
            
            if (value == null) {
                log.debug("OTP not found for key: {}", key);
                return Optional.empty();
            }
            
            OtpData otpData = objectMapper.readValue(value, OtpData.class);
            log.debug("Retrieved OTP for key: {}", key);
            return Optional.of(otpData);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize OTP data", e);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to retrieve OTP from Redis", e);
            return Optional.empty();
        }
    }

    @Override
    public boolean delete(String phone, String requestId) {
        try {
            String key = buildKey(phone, requestId);
            Boolean result = redisTemplate.delete(key);
            log.debug("Deleted OTP for key: {}, result: {}", key, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Failed to delete OTP from Redis", e);
            return false;
        }
    }

    @Override
    public boolean update(OtpData otpData, int ttlSeconds) {
        try {
            String key = buildKey(otpData.getPhone(), otpData.getRequestId());
            String value = objectMapper.writeValueAsString(otpData);
            
            // Update with new TTL
            redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
            log.debug("Updated OTP for key: {}", key);
            return true;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OTP data", e);
            return false;
        } catch (Exception e) {
            log.error("Failed to update OTP in Redis", e);
            return false;
        }
    }

    @Override
    public boolean exists(String phone, String requestId) {
        try {
            String key = buildKey(phone, requestId);
            Boolean result = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Failed to check OTP existence in Redis", e);
            return false;
        }
    }

    /**
     * Build Redis key from phone and request ID
     * Format: otp:{phone}:{requestId}
     */
    private String buildKey(String phone, String requestId) {
        return KEY_PREFIX + phone + ":" + requestId;
    }
}

