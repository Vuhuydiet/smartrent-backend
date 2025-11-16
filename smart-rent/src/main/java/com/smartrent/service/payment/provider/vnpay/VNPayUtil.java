package com.smartrent.service.payment.provider.vnpay;

import com.smartrent.utility.PaymentUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VNPayUtil {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

    /**
     * Generate VNPay secure hash (expects raw values in params)
     * - Excludes vnp_SecureHash and vnp_SecureHashType automatically.
     * - Sorts keys alphabetically.
     * - Encodes values using URLEncoder (Java's encoding uses + for spaces).
     * - Joins pairs with single '&' (no extra ampersands for empty fields).
     */
    public static String generateSecureHash(Map<String, String> params, String secretKey) {
        // Make a defensive copy and remove secure-hash fields
        Map<String, String> copy = new HashMap<>(params);
        copy.remove("vnp_SecureHash");
        copy.remove("vnp_SecureHashType");

        // Collect non-empty keys and sort
        List<String> fieldNames = new ArrayList<>();
        for (String k : copy.keySet()) {
            String v = copy.get(k);
            if (v != null && !v.isEmpty()) {
                fieldNames.add(k);
            }
        }
        Collections.sort(fieldNames);

        // Build canonical string using StringJoiner to avoid stray '&'
        StringJoiner joiner = new StringJoiner("&");
        for (String fieldName : fieldNames) {
            String fieldValue = copy.get(fieldName);
            // encode value once here
            String encoded = URLEncoder.encode(fieldValue, StandardCharsets.UTF_8);
            // URLEncoder encodes spaces as '+', but in case any '%20' exists (from pre-encoded values), normalize
            encoded = encoded.replaceAll("%20", "+");
            joiner.add(fieldName + "=" + encoded);
        }

        String hashInput = joiner.toString();
        log.debug("VNPay hash input: {}", hashInput);

        // HMAC-SHA512
        String hash = PaymentUtil.hmacSHA512(secretKey, hashInput);
        if (hash == null) {
            return null;
        }
        // Normalize hex case to lower-case to match common VNPay examples
        hash = hash.toLowerCase(Locale.ROOT);
        log.debug("VNPay generated hash: {}", hash);
        return hash;
    }

    /**
     * Validate VNPay secure hash
     */
    public static boolean validateSecureHash(Map<String, String> params, String secretKey, String secureHash) {
        try {
            if (secureHash == null) return false;
            String generatedHash = generateSecureHash(params, secretKey);
            if (generatedHash == null) return false;
            return generatedHash.equalsIgnoreCase(secureHash);
        } catch (Exception e) {
            log.error("Error validating secure hash", e);
            return false;
        }
    }

    /**
     * Build URL query string from parameters (expects raw values). Uses same encoding & ordering as hashing.
     */
    public static String buildQueryString(Map<String, String> params) {
        // Collect non-empty keys and sort
        List<String> fieldNames = new ArrayList<>();
        for (String k : params.keySet()) {
            String v = params.get(k);
            if (v != null && !v.isEmpty()) {
                fieldNames.add(k);
            }
        }
        Collections.sort(fieldNames);

        StringJoiner joiner = new StringJoiner("&");
        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            String encoded = URLEncoder.encode(fieldValue, StandardCharsets.UTF_8);
            encoded = encoded.replaceAll("%20", "+");
            joiner.add(fieldName + "=" + encoded);
        }

        return joiner.toString();
    }

    /**
     * Generate VNPay request ID
     */
    public static String generateRequestId() {
        return "REQ" + System.currentTimeMillis() + "_" + PaymentUtil.generateRandomString(8);
    }

    /**
     * VNPay amount format (multiply by 100)
     */
    public static String formatAmount(Long amount) {
        return String.valueOf(amount * 100);
    }

    public static Long parseAmount(String amount) {
        try {
            return Long.parseLong(amount) / 100;
        } catch (NumberFormatException e) {
            log.error("Error parsing amount: {}", amount, e);
            return 0L;
        }
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    public static String formatCurrentDateTime() {
        return formatDateTime(LocalDateTime.now());
    }

    public static Date parseDateTime(String dateTimeString) {
        try {
            return DATE_FORMAT.parse(dateTimeString);
        } catch (Exception e) {
            log.error("Error parsing date time: {}", dateTimeString, e);
            return new Date();
        }
    }

    public static boolean isSuccessResponseCode(String responseCode) {
        return "00".equals(responseCode);
    }
}
