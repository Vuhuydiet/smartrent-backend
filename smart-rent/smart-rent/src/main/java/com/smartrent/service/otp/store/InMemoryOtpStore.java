package com.smartrent.service.otp.store;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * In-memory implementation of OTP store for testing and development
 * Uses ConcurrentHashMap with scheduled cleanup of expired entries
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "otp.store.type", havingValue = "memory")
public class InMemoryOtpStore implements OtpStore {

    private final Map<String, OtpData> storage = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupScheduler;

    public InMemoryOtpStore() {
        // Schedule cleanup task every 60 seconds
        this.cleanupScheduler = Executors.newSingleThreadScheduledExecutor();
        this.cleanupScheduler.scheduleAtFixedRate(
            this::cleanupExpiredEntries,
            60, 60, TimeUnit.SECONDS
        );
    }

    @Override
    public boolean store(OtpData otpData, int ttlSeconds) {
        try {
            String key = buildKey(otpData.getPhone(), otpData.getRequestId());
            
            // Only store if key doesn't exist (atomic operation)
            OtpData existing = storage.putIfAbsent(key, otpData);
            
            boolean stored = existing == null;
            log.debug("Stored OTP for key: {}, result: {}", key, stored);
            return stored;
        } catch (Exception e) {
            log.error("Failed to store OTP in memory", e);
            return false;
        }
    }

    @Override
    public Optional<OtpData> retrieve(String phone, String requestId) {
        try {
            String key = buildKey(phone, requestId);
            OtpData otpData = storage.get(key);
            
            if (otpData == null) {
                log.debug("OTP not found for key: {}", key);
                return Optional.empty();
            }
            
            // Check if expired
            if (otpData.getExpiresAt().isBefore(Instant.now())) {
                log.debug("OTP expired for key: {}", key);
                storage.remove(key);
                return Optional.empty();
            }
            
            log.debug("Retrieved OTP for key: {}", key);
            return Optional.of(otpData);
        } catch (Exception e) {
            log.error("Failed to retrieve OTP from memory", e);
            return Optional.empty();
        }
    }

    @Override
    public boolean delete(String phone, String requestId) {
        try {
            String key = buildKey(phone, requestId);
            OtpData removed = storage.remove(key);
            boolean deleted = removed != null;
            log.debug("Deleted OTP for key: {}, result: {}", key, deleted);
            return deleted;
        } catch (Exception e) {
            log.error("Failed to delete OTP from memory", e);
            return false;
        }
    }

    @Override
    public boolean update(OtpData otpData, int ttlSeconds) {
        try {
            String key = buildKey(otpData.getPhone(), otpData.getRequestId());
            storage.put(key, otpData);
            log.debug("Updated OTP for key: {}", key);
            return true;
        } catch (Exception e) {
            log.error("Failed to update OTP in memory", e);
            return false;
        }
    }

    @Override
    public boolean exists(String phone, String requestId) {
        try {
            String key = buildKey(phone, requestId);
            OtpData otpData = storage.get(key);
            
            if (otpData == null) {
                return false;
            }
            
            // Check if expired
            if (otpData.getExpiresAt().isBefore(Instant.now())) {
                storage.remove(key);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("Failed to check OTP existence in memory", e);
            return false;
        }
    }

    /**
     * Build key from phone and request ID
     */
    private String buildKey(String phone, String requestId) {
        return phone + ":" + requestId;
    }

    /**
     * Cleanup expired entries
     */
    private void cleanupExpiredEntries() {
        try {
            Instant now = Instant.now();
            storage.entrySet().removeIf(entry -> 
                entry.getValue().getExpiresAt().isBefore(now)
            );
            log.debug("Cleaned up expired OTP entries");
        } catch (Exception e) {
            log.error("Failed to cleanup expired OTP entries", e);
        }
    }

    /**
     * Shutdown cleanup scheduler
     */
    public void shutdown() {
        cleanupScheduler.shutdown();
    }
}

