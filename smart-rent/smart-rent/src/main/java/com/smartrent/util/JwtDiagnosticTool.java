package com.smartrent.util;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.Date;

@Slf4j
@Component
public class JwtDiagnosticTool {

    @Value("${application.authentication.jwt.access-signer-key}")
    private String ACCESS_SIGNER_KEY;

    @Value("${application.authentication.jwt.refresh-signer-key}")
    private String REFRESH_SIGNER_KEY;

    @Value("${application.authentication.jwt.reset-password-signer-key}")
    private String RESET_PASSWORD_SIGNER_KEY;

    public void diagnoseToken(String token) {
        log.info("=== JWT Token Diagnostic ===");
        
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            
            // Basic token info
            log.info("Token Header: {}", signedJWT.getHeader().toJSONObject());
            log.info("Token Claims: {}", signedJWT.getJWTClaimsSet().toJSONObject());
            
            // Check expiration
            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            Date now = new Date();
            log.info("Token expiration: {}", expirationTime);
            log.info("Current time: {}", now);
            log.info("Token expired: {}", expirationTime.before(now));
            
            // Test signature with different keys
            testSignatureWithKey(signedJWT, ACCESS_SIGNER_KEY, "ACCESS_SIGNER_KEY");
            testSignatureWithKey(signedJWT, REFRESH_SIGNER_KEY, "REFRESH_SIGNER_KEY");
            testSignatureWithKey(signedJWT, RESET_PASSWORD_SIGNER_KEY, "RESET_PASSWORD_SIGNER_KEY");
            
        } catch (ParseException e) {
            log.error("Failed to parse JWT token: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error during token diagnosis: {}", e.getMessage());
        }
        
        log.info("=== End Diagnostic ===");
    }
    
    private void testSignatureWithKey(SignedJWT signedJWT, String key, String keyName) {
        try {
            JWSVerifier verifier = new MACVerifier(key);
            boolean verified = signedJWT.verify(verifier);
            log.info("Signature verification with {}: {}", keyName, verified);
        } catch (JOSEException e) {
            log.error("Error verifying signature with {}: {}", keyName, e.getMessage());
        }
    }
    
    public void printKeyInfo() {
        log.info("=== JWT Key Information ===");
        log.info("ACCESS_SIGNER_KEY length: {}", ACCESS_SIGNER_KEY != null ? ACCESS_SIGNER_KEY.length() : "null");
        log.info("REFRESH_SIGNER_KEY length: {}", REFRESH_SIGNER_KEY != null ? REFRESH_SIGNER_KEY.length() : "null");
        log.info("RESET_PASSWORD_SIGNER_KEY length: {}", RESET_PASSWORD_SIGNER_KEY != null ? RESET_PASSWORD_SIGNER_KEY.length() : "null");
        
        // Print first and last few characters for debugging (don't print full key for security)
        if (ACCESS_SIGNER_KEY != null && ACCESS_SIGNER_KEY.length() > 10) {
            log.info("ACCESS_SIGNER_KEY preview: {}...{}", 
                ACCESS_SIGNER_KEY.substring(0, 5), 
                ACCESS_SIGNER_KEY.substring(ACCESS_SIGNER_KEY.length() - 5));
        }
        log.info("=== End Key Information ===");
    }
}
