package com.smartrent.validator;

import com.smartrent.config.VNPayConfig;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
     * Build signature data from decoded parameters: sort keys alphabetically and
     * URL-encode each value with US_ASCII before joining with `=` and `&`.
     * Matches VNPay's official Java sample (Config.hashAllFields).
     */
    private String buildSignatureData(Map<String, String> params) {
        try {
            List<String> fieldNames = new ArrayList<>(params.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = params.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    hashData.append(fieldName)
                            .append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    if (itr.hasNext()) {
                        hashData.append('&');
                    }
                }
            }
            return hashData.toString();
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to URL-encode VNPay signature data", e);
        }
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

