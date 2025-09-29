package com.smartrent.service.user;

public interface UserVerificationService {
    /**
     * Check if the current authenticated user's phone number is active/verified
     * Uses SecurityContextHolder to get current user from authentication context
     * @return true if phone is verified, false otherwise
     */
    boolean isPhoneActive();
}