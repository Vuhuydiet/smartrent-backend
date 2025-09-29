package com.smartrent.utility;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class AuthorizationUtil {

    /**
     * Get current authenticated user ID
     */
    public static String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }
        return authentication.getName();
    }

    /**
     * Check if current user is an admin
     */
    public static boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return authorities.stream()
                .anyMatch(auth -> auth.getAuthority().equals("SCOPE_ADMIN") 
                               || auth.getAuthority().equals("ADMIN")
                               || auth.getAuthority().equals("SCOPE_SA")
                               || auth.getAuthority().equals("SA"));
    }

    /**
     * Check if current user can access listing (admin or owner)
     */
    public static boolean canAccessListing(String listingOwnerId) {
        if (isCurrentUserAdmin()) {
            return true;
        }
        String currentUserId = getCurrentUserId();
        return currentUserId.equals(listingOwnerId);
    }

    /**
     * Check if current user can modify listing status (admin only)
     */
    public static boolean canModifyListingStatus() {
        return isCurrentUserAdmin();
    }
}