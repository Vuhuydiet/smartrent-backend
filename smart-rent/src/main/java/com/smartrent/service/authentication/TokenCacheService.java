package com.smartrent.service.authentication;

import java.time.LocalDateTime;


public interface TokenCacheService {


    void invalidateToken(String tokenId, LocalDateTime expirationTime);

    void invalidateTokens(String accessTokenId, String refreshTokenId, LocalDateTime expirationTime);

    boolean isTokenInvalidated(String tokenId);

    void removeInvalidatedToken(String tokenId);

    void clearAllInvalidatedTokens();
}
