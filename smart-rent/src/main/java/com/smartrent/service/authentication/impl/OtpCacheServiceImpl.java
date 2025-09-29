package com.smartrent.service.authentication.impl;

import com.smartrent.config.Constants;
import com.smartrent.service.authentication.OtpCacheService;
import com.smartrent.service.authentication.domain.OtpData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpCacheServiceImpl implements OtpCacheService {

    private final CacheManager cacheManager;
    
    @Override
    public void storeOtp(OtpData otpData) {
        try {
            Cache cache = cacheManager.getCache(Constants.CacheNames.OTP);
            if (cache != null) {
                String otpKey = Constants.CacheKeys.buildOtpKey(otpData.getUserId(), otpData.getOtpCode());
                cache.put(otpKey, otpData);

                String userKey = Constants.CacheKeys.buildUserKey(otpData.getUserId());
                cache.put(userKey, otpKey);

                String emailKey = Constants.CacheKeys.buildEmailKey(otpData.getUserEmail());
                cache.put(emailKey, otpKey);

                log.debug("OTP stored in cache for user: {}, key: {}, expires at: {}",
                    otpData.getUserEmail(), otpKey, otpData.getExpirationTime());
            } else {
                log.error("OTP cache not found, OTP storage failed");
            }
        } catch (Exception e) {
            log.error("Failed to store OTP in cache for user: {}", otpData.getUserEmail(), e);
            throw new RuntimeException("Failed to store OTP", e);
        }
    }
    
    @Override
    public Optional<OtpData> getOtp(String otpCode, String userEmail) {
        try {
            Cache cache = cacheManager.getCache(Constants.CacheNames.OTP);
            if (cache != null) {
                return findOtpByEmailAndCode(cache, otpCode, userEmail);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to retrieve OTP from cache for user: {}", userEmail, e);
            return Optional.empty();
        }
    }
    
    @Override
    public void removeOtp(String userId, String otpCode) {
        try {
            Cache cache = cacheManager.getCache(Constants.CacheNames.OTP);
            if (cache != null) {
                String otpKey = Constants.CacheKeys.buildOtpKey(userId, otpCode);

                Cache.ValueWrapper otpWrapper = cache.get(otpKey);
                if (otpWrapper != null && otpWrapper.get() instanceof OtpData otpData) {
                    String emailKey = Constants.CacheKeys.buildEmailKey(otpData.getUserEmail());
                    cache.evict(emailKey);
                }

                cache.evict(otpKey);
                cache.evict(Constants.CacheKeys.buildUserKey(userId));

                log.info("OTP removed from cache: userId={}, otpCode={}", userId, otpCode);
            }
        } catch (Exception e) {
            log.error("Failed to remove OTP from cache: userId={}, otpCode={}", userId, otpCode, e);
        }
    }
    
    @Override
    public void removeUserOtps(String userId) {
        try {
            Cache cache = cacheManager.getCache(Constants.CacheNames.OTP);
            if (cache != null) {
                String userKey = Constants.CacheKeys.buildUserKey(userId);
                Cache.ValueWrapper wrapper = cache.get(userKey);
                if (wrapper != null && wrapper.get() instanceof String otpKey) {

                    Cache.ValueWrapper otpWrapper = cache.get(otpKey);
                    if (otpWrapper != null && otpWrapper.get() instanceof OtpData otpData) {
                        String emailKey = Constants.CacheKeys.buildEmailKey(otpData.getUserEmail());
                        cache.evict(emailKey);
                    }

                    cache.evict(otpKey);
                    cache.evict(userKey);
                    log.info("User OTPs removed from cache for user: {}", userId);
                }
            }
        } catch (Exception e) {
            log.error("Failed to remove user OTPs from cache for user: {}", userId, e);
        }
    }
    
    @Override
    public boolean isValidOtp(String otpCode, String userEmail) {
        return getOtp(otpCode, userEmail).isPresent();
    }

    private Optional<OtpData> findOtpByEmailAndCode(Cache cache, String otpCode, String userEmail) {
        String emailKey = Constants.CacheKeys.buildEmailKey(userEmail);
        Cache.ValueWrapper emailWrapper = cache.get(emailKey);
        if (emailWrapper != null && emailWrapper.get() instanceof String otpKey) {
            Cache.ValueWrapper otpWrapper = cache.get(otpKey);
            if (otpWrapper != null && otpWrapper.get() instanceof OtpData otpData) {
                if (otpCode.equals(otpData.getOtpCode()) && !otpData.isExpired()) {
                    log.debug("Valid OTP found for user: {}", userEmail);
                    return Optional.of(otpData);
                } else if (otpData.isExpired()) {
                    cache.evict(otpKey);
                    cache.evict(emailKey);
                    cache.evict(Constants.CacheKeys.buildUserKey(otpData.getUserId()));
                    log.debug("Expired OTP removed for user: {}", userEmail);
                }
            }
        }
        return Optional.empty();
    }
}
