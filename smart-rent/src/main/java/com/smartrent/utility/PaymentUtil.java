package com.smartrent.utility;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Slf4j
public class PaymentUtil {

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Random RANDOM = new SecureRandom();
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Generate unique transaction reference
     */
    public static String generateTxnRef() {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String randomPart = generateRandomString(8);
        return timestamp + "_" + randomPart;
    }

    /**
     * Generate random alphanumeric string
     */
    public static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    /**
     * Get client IP address from headers or remote address
     */
    public static String getClientIpAddress(String xForwardedFor, String xRealIp, String remoteAddr) {
        // Check X-Forwarded-For header first (for proxied requests)
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            String[] ips = xForwardedFor.split(",");
            return ips[0].trim();
        }

        // Check X-Real-IP header
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp.trim();
        }

        // Fallback to remote address
        return remoteAddr != null ? remoteAddr : "0.0.0.0";
    }

    /**
     * Validate amount is positive
     */
    public static boolean isValidAmount(Long amount) {
        return amount != null && amount > 0;
    }

    /**
     * Validate currency code
     */
    public static boolean isValidCurrency(String currency) {
        if (currency == null || currency.isEmpty()) {
            return false;
        }
        // Support common currencies
        return currency.matches("^(VND|USD|EUR|GBP|JPY|CNY)$");
    }

    /**
     * Format amount for display
     */
    public static String formatAmount(Long amount, String currency) {
        if (amount == null) {
            return "0";
        }

        if ("VND".equals(currency)) {
            return String.format("%,d đ", amount);
        } else {
            return String.format("%,.2f %s", amount / 100.0, currency);
        }
    }

    /**
     * Mask sensitive data for logging
     */
    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 8) {
            return "****";
        }
        return cardNumber.substring(0, 4) + "****" + cardNumber.substring(cardNumber.length() - 4);
    }

    /**
     * Generate order info with max length
     */
    public static String truncateOrderInfo(String orderInfo, int maxLength) {
        if (orderInfo == null) {
            return "";
        }
        if (orderInfo.length() <= maxLength) {
            return orderInfo;
        }
        return orderInfo.substring(0, maxLength - 3) + "...";
    }

    /**
     * Check if transaction reference is valid format
     */
    public static boolean isValidTransactionRef(String transactionRef) {
        if (transactionRef == null || transactionRef.isEmpty()) {
            return false;
        }
        // Should match pattern: PREFIX_TIMESTAMP_RANDOM
        return transactionRef.matches("^[A-Z]+_\\d{14}_[A-Z0-9]{8}$");
    }

    /**
     * Extract provider from transaction reference
     */
    public static String extractProviderFromTxnRef(String transactionRef) {
        if (transactionRef == null || !transactionRef.contains("_")) {
            return null;
        }
        return transactionRef.split("_")[0];
    }

    /**
     * Generate HMAC-SHA512 hash
     */
    public static String hmacSHA512(String key, String data) {
        try {
            Mac hmacSHA512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
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
        } catch (Exception e) {
            log.error("Error generating HMAC-SHA512", e);
            throw new RuntimeException("Failed to generate HMAC-SHA512", e);
        }
    }
}

