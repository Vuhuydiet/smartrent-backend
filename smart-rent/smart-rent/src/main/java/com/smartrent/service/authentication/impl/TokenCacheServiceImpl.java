package com.smartrent.service.authentication.impl;

import com.smartrent.config.Constants;
import com.smartrent.service.authentication.TokenCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenCacheServiceImpl implements TokenCacheService {

    private final CacheManager cacheManager;
    
    @Override
    public void invalidateToken(String tokenId, LocalDateTime expirationTime) {
        try {
            Cache cache = cacheManager.getCache(Constants.CacheNames.INVALIDATED_TOKENS);
            if (cache != null) {
                // Store the token ID with its expiration time
                // The cache TTL will handle automatic cleanup
                cache.put(tokenId, expirationTime.toString());
                log.debug("Token invalidated in cache: {}", tokenId);
            } else {
                log.warn("Invalidated tokens cache not found, token invalidation may not work properly");
            }
        } catch (Exception e) {
            log.error("Failed to invalidate token in cache: {}", tokenId, e);
            // Don't throw exception to avoid breaking the authentication flow
            // In case of cache failure, tokens will still expire naturally
        }
    }
    
    @Override
    public void invalidateTokens(String accessTokenId, String refreshTokenId, LocalDateTime expirationTime) {
        invalidateToken(accessTokenId, expirationTime);
        if (refreshTokenId != null && !refreshTokenId.equals(accessTokenId)) {
            invalidateToken(refreshTokenId, expirationTime);
        }
    }
    
    @Override
    public boolean isTokenInvalidated(String tokenId) {
        try {
            Cache cache = cacheManager.getCache(Constants.CacheNames.INVALIDATED_TOKENS);
            if (cache != null) {
                Cache.ValueWrapper wrapper = cache.get(tokenId);
                boolean isInvalidated = wrapper != null;
                log.debug("Token invalidation check for {}: {}", tokenId, isInvalidated);
                return isInvalidated;
            } else {
                log.warn("Invalidated tokens cache not found, assuming token is valid");
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to check token invalidation status for: {}", tokenId, e);
            // In case of cache failure, assume token is valid to avoid breaking authentication
            return false;
        }
    }
    
    @Override
    public void removeInvalidatedToken(String tokenId) {
        try {
            Cache cache = cacheManager.getCache(Constants.CacheNames.INVALIDATED_TOKENS);
            if (cache != null) {
                cache.evict(tokenId);
                log.debug("Token removed from invalidation cache: {}", tokenId);
            }
        } catch (Exception e) {
            log.error("Failed to remove token from invalidation cache: {}", tokenId, e);
        }
    }
    
    @Override
    public void clearAllInvalidatedTokens() {
        try {
            Cache cache = cacheManager.getCache(Constants.CacheNames.INVALIDATED_TOKENS);
            if (cache != null) {
                cache.clear();
                log.info("All invalidated tokens cleared from cache");
            }
        } catch (Exception e) {
            log.error("Failed to clear invalidated tokens cache", e);
        }
    }
}
