package com.smartrent.validator;

import com.smartrent.config.VNPayConfig;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Validator for VNPay signatures
 * Verifies HMAC-SHA512 signatures from VNPay callbacks
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VNPaySignatureValidator {

    VNPayConfig vnPayConfig;

    /**
     * Verify VNPay callback signature
     */
    public boolean verifySignature(Map<String, String> params, String receivedSignature) {
        try {
            // Remove signature and hash type from params
            Map<String, String> signParams = new TreeMap<>(params);
            signParams.remove("vnp_SecureHash");
            signParams.remove("vnp_SecureHashType");

            // Build signature data
            String signData = buildSignatureData(signParams);

            // Generate signature
            String generatedSignature = generateHmacSHA512(signData, vnPayConfig.getHashSecret());

            // Compare signatures
            boolean isValid = generatedSignature.equalsIgnoreCase(receivedSignature);

            if (!isValid) {
                log.warn("VNPay signature verification failed. Expected: {}, Got: {}", 
                        generatedSignature, receivedSignature);
            }

            return isValid;

        } catch (Exception e) {
            log.error("Error verifying VNPay signature", e);
            return false;
        }
    }

    /**
     * Build signature data from parameters
     */
    private String buildSignatureData(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }

    /**
     * Generate HMAC-SHA512 signature
     */
    private String generateHmacSHA512(String data, String secret) throws Exception {
        Mac hmacSHA512 = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        hmacSHA512.init(secretKeySpec);
        byte[] hash = hmacSHA512.doFinal(data.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }

    /**
     * Validate signature format
     */
    public boolean isValidSignatureFormat(String signature) {
        if (signature == null || signature.isEmpty()) {
            log.warn("Signature is null or empty");
            return false;
        }

        // VNPay HMAC-SHA512 signature should be 128 hex characters
        if (signature.length() != 128) {
            log.warn("Invalid signature length: {}", signature.length());
            return false;
        }

        // Check if signature contains only hex characters
        if (!signature.matches("[0-9a-fA-F]+")) {
            log.warn("Signature contains non-hex characters");
            return false;
        }

        return true;
    }
}

