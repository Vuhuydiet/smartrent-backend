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
     * Generate VNPay secure hash
     */
    public static String generateSecureHash(Map<String, String> params, String secretKey) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                if (itr.hasNext()) {
                    hashData.append('&');
                }
            }
        }

        return PaymentUtil.hmacSHA512(secretKey, hashData.toString());
    }

    /**
     * Validate VNPay secure hash
     */
    public static boolean validateSecureHash(Map<String, String> params, String secretKey, String secureHash) {
        try {
            // Remove secure hash from params for validation
            Map<String, String> validationParams = new HashMap<>(params);
            validationParams.remove("vnp_SecureHash");
            validationParams.remove("vnp_SecureHashType");

            String generatedHash = generateSecureHash(validationParams, secretKey);
            return generatedHash.equals(secureHash);
        } catch (Exception e) {
            log.error("Error validating secure hash", e);
            return false;
        }
    }

    /**
     * Build query string from parameters
     */
    public static String buildQueryString(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                if (itr.hasNext()) {
                    query.append('&');
                }
            }
        }
        return query.toString();
    }

    /**
     * Generate VNPay request ID
     */
    public static String generateRequestId() {
        return "REQ" + System.currentTimeMillis() + "_" + PaymentUtil.generateRandomString(8);
    }

    /**
     * Format amount to VNPay format (multiply by 100 for cents)
     */
    public static String formatAmount(Long amount) {
        return String.valueOf(amount * 100);
    }

    /**
     * Parse amount from VNPay format (divide by 100 from cents)
     */
    public static Long parseAmount(String amount) {
        try {
            return Long.parseLong(amount) / 100;
        } catch (NumberFormatException e) {
            log.error("Error parsing amount: {}", amount, e);
            return 0L;
        }
    }

    /**
     * Format date time for VNPay
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    /**
     * Format current date time for VNPay
     */
    public static String formatCurrentDateTime() {
        return formatDateTime(LocalDateTime.now());
    }

    /**
     * Parse VNPay date time
     */
    public static Date parseDateTime(String dateTimeString) {
        try {
            return DATE_FORMAT.parse(dateTimeString);
        } catch (Exception e) {
            log.error("Error parsing date time: {}", dateTimeString, e);
            return new Date();
        }
    }

    /**
     * Validate VNPay response code
     */
    public static boolean isSuccessResponseCode(String responseCode) {
        return "00".equals(responseCode);
    }
}
