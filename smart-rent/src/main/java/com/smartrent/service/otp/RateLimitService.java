package com.smartrent.service.otp;

import com.smartrent.config.otp.OtpProperties;
import com.smartrent.infra.exception.OtpRateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service for rate limiting OTP requests
 * Implements sliding window rate limiting using Redis
 * 
 * Rate limits:
 * - Per phone: 5 sends per hour (configurable)
 * - Per IP: 20 sends per hour (configurable)
 * 
 * TODO: Consider integrating CAPTCHA on high-volume flows to prevent abuse
 */
@Slf4j
@Service
public class RateLimitService {

    private static final String PHONE_RATE_LIMIT_KEY_PREFIX = "otp:ratelimit:phone:";
    private static final String IP_RATE_LIMIT_KEY_PREFIX = "otp:ratelimit:ip:";

    private final OtpProperties otpProperties;
    private final RedisTemplate<String, String> redisTemplate;

    public RateLimitService(OtpProperties otpProperties, RedisTemplate<String, String> redisTemplate) {
        this.otpProperties = otpProperties;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Check and increment rate limit for phone number
     *
     * @param phone Phone number in E.164 format
     * @throws OtpRateLimitException if rate limit exceeded
     */
    public void checkPhoneRateLimit(String phone) {
        String key = PHONE_RATE_LIMIT_KEY_PREFIX + phone;
        int maxSends = otpProperties.getRateLimit().getMaxSendsPerPhone();
        int windowSeconds = otpProperties.getRateLimit().getWindowSeconds();

        checkAndIncrementRateLimit(key, maxSends, windowSeconds, "phone", phone);
    }

    /**
     * Check and increment rate limit for IP address
     *
     * @param ipAddress IP address
     * @throws OtpRateLimitException if rate limit exceeded
     */
    public void checkIpRateLimit(String ipAddress) {
        String key = IP_RATE_LIMIT_KEY_PREFIX + ipAddress;
        int maxSends = otpProperties.getRateLimit().getMaxSendsPerIp();
        int windowSeconds = otpProperties.getRateLimit().getWindowSeconds();

        checkAndIncrementRateLimit(key, maxSends, windowSeconds, "IP", ipAddress);
    }

    /**
     * Get remaining attempts for phone number
     *
     * @param phone Phone number in E.164 format
     * @return Remaining attempts
     */
    public int getRemainingPhoneAttempts(String phone) {
        String key = PHONE_RATE_LIMIT_KEY_PREFIX + phone;
        int maxSends = otpProperties.getRateLimit().getMaxSendsPerPhone();
        return getRemainingAttempts(key, maxSends);
    }

    /**
     * Get remaining attempts for IP address
     *
     * @param ipAddress IP address
     * @return Remaining attempts
     */
    public int getRemainingIpAttempts(String ipAddress) {
        String key = IP_RATE_LIMIT_KEY_PREFIX + ipAddress;
        int maxSends = otpProperties.getRateLimit().getMaxSendsPerIp();
        return getRemainingAttempts(key, maxSends);
    }

    /**
     * Reset rate limit for phone number (admin function)
     *
     * @param phone Phone number in E.164 format
     */
    public void resetPhoneRateLimit(String phone) {
        String key = PHONE_RATE_LIMIT_KEY_PREFIX + phone;
        redisTemplate.delete(key);
        log.info("Reset rate limit for phone: {}", maskPhone(phone));
    }

    /**
     * Reset rate limit for IP address (admin function)
     *
     * @param ipAddress IP address
     */
    public void resetIpRateLimit(String ipAddress) {
        String key = IP_RATE_LIMIT_KEY_PREFIX + ipAddress;
        redisTemplate.delete(key);
        log.info("Reset rate limit for IP: {}", ipAddress);
    }

    /**
     * Check and increment rate limit counter
     */
    private void checkAndIncrementRateLimit(String key, int maxAttempts, int windowSeconds, 
                                           String limitType, String identifier) {
        try {
            // Get current count
            String countStr = redisTemplate.opsForValue().get(key);
            int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;

            // Check if limit exceeded
            if (currentCount >= maxAttempts) {
                Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                log.warn("Rate limit exceeded for {} {}: count={}, max={}, ttl={}s", 
                    limitType, maskIdentifier(identifier), currentCount, maxAttempts, ttl);
                throw new OtpRateLimitException();
            }

            // Increment counter
            Long newCount = redisTemplate.opsForValue().increment(key);
            
            // Set TTL if this is the first increment
            if (newCount != null && newCount == 1) {
                redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
            }

            log.debug("Rate limit check passed for {} {}: count={}/{}", 
                limitType, maskIdentifier(identifier), newCount, maxAttempts);

        } catch (OtpRateLimitException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to check rate limit for {} {}", limitType, identifier, e);
            // Fail open: allow request if rate limit check fails
            // In production, you might want to fail closed for security
        }
    }

    /**
     * Get remaining attempts
     */
    private int getRemainingAttempts(String key, int maxAttempts) {
        try {
            String countStr = redisTemplate.opsForValue().get(key);
            int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;
            return Math.max(0, maxAttempts - currentCount);
        } catch (Exception e) {
            log.error("Failed to get remaining attempts for key: {}", key, e);
            return maxAttempts;
        }
    }

    /**
     * Mask phone number for logging
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 8) {
            return phone;
        }
        return phone.substring(0, 5) + "***" + phone.substring(phone.length() - 3);
    }

    /**
     * Mask identifier for logging
     */
    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.length() < 8) {
            return identifier;
        }
        if (identifier.startsWith("+")) {
            return maskPhone(identifier);
        }
        return identifier;
    }
}

