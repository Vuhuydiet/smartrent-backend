package com.smartrent.service.user.impl;

import com.smartrent.service.user.UserVerificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserVerificationServiceImpl implements UserVerificationService {

    @Override
    public boolean isPhoneActive() {
        // Get current authenticated user from security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("No authenticated user found for phone verification check");
            return false;
        }
        
        String currentUserId = authentication.getName();
        
        // TEMPORARY MOCKING: Until actual verification data is available,
        // hardcode the check to isActiveNumber = true for all user requests 
        // to allow immediate testing.
        log.info("Checking phone verification status for user: {} (Currently mocked as true)", currentUserId);
        return true;
        
        // TODO: Replace with actual implementation when phone verification system is ready
        // This should check the user's phone verification status from database
        // return userRepository.findById(currentUserId)
        //         .map(User::getIsPhoneActive)
        //         .orElse(false);
    }
}