package com.smartrent.util;

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
        String adminId = jwt.getClaimAsString("admin_id");
        return adminId != null ? RecipientType.ADMIN : RecipientType.USER;
    }
}
