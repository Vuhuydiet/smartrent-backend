package com.smartrent.service.otp.store;

import java.util.Optional;

/**
 * Interface for OTP storage operations
 * Implementations should ensure atomic operations and TTL support
 */
public interface OtpStore {

    /**
     * Store OTP data with TTL
     *
     * @param otpData OTP data to store
     * @param ttlSeconds Time-to-live in seconds
     * @return true if stored successfully, false if key already exists
     */
    boolean store(OtpData otpData, int ttlSeconds);

    /**
     * Retrieve OTP data by phone and request ID
     *
     * @param phone Phone number in E.164 format
     * @param requestId Request ID
     * @return Optional containing OTP data if found
     */
    Optional<OtpData> retrieve(String phone, String requestId);

    /**
     * Delete OTP data
     *
     * @param phone Phone number in E.164 format
     * @param requestId Request ID
     * @return true if deleted, false if not found
     */
    boolean delete(String phone, String requestId);

    /**
     * Update OTP data (for incrementing attempts, marking as verified, etc.)
     *
     * @param otpData Updated OTP data
     * @param ttlSeconds Remaining TTL in seconds
     * @return true if updated successfully
     */
    boolean update(OtpData otpData, int ttlSeconds);

    /**
     * Check if OTP exists
     *
     * @param phone Phone number in E.164 format
     * @param requestId Request ID
     * @return true if exists
     */
    boolean exists(String phone, String requestId);
}

