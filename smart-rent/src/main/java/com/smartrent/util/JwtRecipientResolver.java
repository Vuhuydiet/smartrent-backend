package com.smartrent.util;

import com.smartrent.config.Constants;
import com.smartrent.enums.RecipientType;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Single source of truth for deriving a notification recipientId/recipientType
 * from a JWT. Used by both the REST notification endpoints and the WebSocket
 * STOMP CONNECT authenticator so the Principal name pinned to a session always
 * matches the recipientId notifications are addressed to via
 * {@code convertAndSendToUser}. Keeping this logic in one place avoids the two
 * call sites silently drifting apart.
 */
public final class JwtRecipientResolver {

    private JwtRecipientResolver() {
    }

    public static String resolveRecipientId(Jwt jwt) {
        String adminId = jwt.getClaimAsString("admin_id");
        if (adminId != null) {
            return adminId;
        }

        String userId = jwt.getClaimAsString("user_id");
        if (userId != null) {
            return userId;
        }

        return jwt.getSubject();
    }

    public static RecipientType resolveRecipientType(Jwt jwt) {
        if (jwt.getClaimAsString("admin_id") != null) {
            return RecipientType.ADMIN;
        }

        // Fallback for admin access tokens minted before the admin_id claim was
        // added: a user token carries scope exactly "ROLE_USER", while an admin
        // token carries its admin role scopes (ROLE_SA / ROLE_UA / ROLE_SPA).
        // Anything that isn't the plain user scope is an admin.
        String scope = jwt.getClaimAsString("scope");
        if (scope != null && !scope.isBlank()
                && !Constants.ROLE_USER.equals(scope.trim())) {
            return RecipientType.ADMIN;
        }

        return RecipientType.USER;
    }
}
