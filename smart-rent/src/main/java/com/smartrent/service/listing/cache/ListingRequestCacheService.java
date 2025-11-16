package com.smartrent.service.listing.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartrent.dto.request.ListingCreationRequest;
import com.smartrent.dto.request.VipListingCreationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service to cache listing creation requests in Redis pending payment completion
 * Supports both NORMAL and VIP listing requests
 */
@Slf4j
@Service
public class ListingRequestCacheService {

    private static final String NORMAL_LISTING_PREFIX = "listing:normal:";
    private static final String VIP_LISTING_PREFIX = "listing:vip:";
    private static final int DEFAULT_TTL_MINUTES = 30;

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public ListingRequestCacheService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Store NORMAL listing creation request in cache
     * @param transactionId Transaction ID used as cache key
     * @param request Listing creation request
     * @return true if stored successfully
     */
    public boolean storeNormalListingRequest(String transactionId, ListingCreationRequest request) {
        try {
            String key = buildNormalListingKey(transactionId);
            String value = objectMapper.writeValueAsString(request);

            redisTemplate.opsForValue().set(key, value, DEFAULT_TTL_MINUTES, TimeUnit.MINUTES);
            log.info("Cached NORMAL listing request for transaction: {}", transactionId);
            return true;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize NORMAL listing request for transaction: {}", transactionId, e);
            return false;
        } catch (Exception e) {
            log.error("Failed to cache NORMAL listing request in Redis for transaction: {}", transactionId, e);
            return false;
        }
    }

    /**
     * Store VIP listing creation request in cache
     * @param transactionId Transaction ID used as cache key
     * @param request VIP listing creation request
     * @return true if stored successfully
     */
    public boolean storeVipListingRequest(String transactionId, VipListingCreationRequest request) {
        try {
            String key = buildVipListingKey(transactionId);
            String value = objectMapper.writeValueAsString(request);

            redisTemplate.opsForValue().set(key, value, DEFAULT_TTL_MINUTES, TimeUnit.MINUTES);
            log.info("Cached VIP listing request for transaction: {}", transactionId);
            return true;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize VIP listing request for transaction: {}", transactionId, e);
            return false;
        } catch (Exception e) {
            log.error("Failed to cache VIP listing request in Redis for transaction: {}", transactionId, e);
            return false;
        }
    }

    /**
     * Retrieve NORMAL listing request from cache
     * @param transactionId Transaction ID
     * @return Listing creation request or null if not found
     */
    public ListingCreationRequest getNormalListingRequest(String transactionId) {
        try {
            String key = buildNormalListingKey(transactionId);
            String value = redisTemplate.opsForValue().get(key);

            if (value == null) {
                log.warn("NORMAL listing request not found in cache for transaction: {}", transactionId);
                return null;
            }

            ListingCreationRequest request = objectMapper.readValue(value, ListingCreationRequest.class);
            log.info("Retrieved NORMAL listing request from cache for transaction: {}", transactionId);
            return request;
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize NORMAL listing request for transaction: {}", transactionId, e);
            return null;
        } catch (Exception e) {
            log.error("Failed to retrieve NORMAL listing request from Redis for transaction: {}", transactionId, e);
            return null;
        }
    }

    /**
     * Retrieve VIP listing request from cache
     * @param transactionId Transaction ID
     * @return VIP listing creation request or null if not found
     */
    public VipListingCreationRequest getVipListingRequest(String transactionId) {
        try {
            String key = buildVipListingKey(transactionId);
            String value = redisTemplate.opsForValue().get(key);

            if (value == null) {
                log.warn("VIP listing request not found in cache for transaction: {}", transactionId);
                return null;
            }

            VipListingCreationRequest request = objectMapper.readValue(value, VipListingCreationRequest.class);
            log.info("Retrieved VIP listing request from cache for transaction: {}", transactionId);
            return request;
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize VIP listing request for transaction: {}", transactionId, e);
            return null;
        } catch (Exception e) {
            log.error("Failed to retrieve VIP listing request from Redis for transaction: {}", transactionId, e);
            return null;
        }
    }

    /**
     * Remove NORMAL listing request from cache
     * @param transactionId Transaction ID
     */
    public void removeNormalListingRequest(String transactionId) {
        try {
            String key = buildNormalListingKey(transactionId);
            Boolean result = redisTemplate.delete(key);
            log.info("Removed NORMAL listing request from cache for transaction: {}, result: {}",
                    transactionId, result);
        } catch (Exception e) {
            log.error("Failed to remove NORMAL listing request from Redis for transaction: {}",
                    transactionId, e);
        }
    }

    /**
     * Remove VIP listing request from cache
     * @param transactionId Transaction ID
     */
    public void removeVipListingRequest(String transactionId) {
        try {
            String key = buildVipListingKey(transactionId);
            Boolean result = redisTemplate.delete(key);
            log.info("Removed VIP listing request from cache for transaction: {}, result: {}",
                    transactionId, result);
        } catch (Exception e) {
            log.error("Failed to remove VIP listing request from Redis for transaction: {}",
                    transactionId, e);
        }
    }

    /**
     * Check if NORMAL listing request exists in cache
     * @param transactionId Transaction ID
     * @return true if exists
     */
    public boolean normalListingRequestExists(String transactionId) {
        try {
            String key = buildNormalListingKey(transactionId);
            Boolean result = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Failed to check NORMAL listing request existence for transaction: {}",
                    transactionId, e);
            return false;
        }
    }

    /**
     * Check if VIP listing request exists in cache
     * @param transactionId Transaction ID
     * @return true if exists
     */
    public boolean vipListingRequestExists(String transactionId) {
        try {
            String key = buildVipListingKey(transactionId);
            Boolean result = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Failed to check VIP listing request existence for transaction: {}",
                    transactionId, e);
            return false;
        }
    }

    private String buildNormalListingKey(String transactionId) {
        return NORMAL_LISTING_PREFIX + transactionId;
    }

    private String buildVipListingKey(String transactionId) {
        return VIP_LISTING_PREFIX + transactionId;
    }
}
