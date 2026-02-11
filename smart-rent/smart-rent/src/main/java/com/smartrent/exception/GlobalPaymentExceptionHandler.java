package com.smartrent.exception;

import com.smartrent.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for payment-related errors
 */
@Slf4j
@RestControllerAdvice
public class GlobalPaymentExceptionHandler {

    /**
     * Handle insufficient quota exception
     */
    @ExceptionHandler(InsufficientQuotaException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleInsufficientQuota(InsufficientQuotaException ex) {
        log.warn("Insufficient quota: {}", ex.getMessage());

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("userId", ex.getUserId());
        errorDetails.put("benefitType", ex.getBenefitType().name());
        errorDetails.put("required", ex.getRequired());
        errorDetails.put("available", ex.getAvailable());
        errorDetails.put("message", ex.getMessage());

        ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                .code("QUOTA_001")
                .message("INSUFFICIENT_QUOTA")
                .data(errorDetails)
                .build();

        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(response);
    }

    /**
     * Handle payment failed exception
     */
    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handlePaymentFailed(PaymentFailedException ex) {
        log.error("Payment failed: {}", ex.getMessage());

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("transactionId", ex.getTransactionId());
        errorDetails.put("providerTransactionId", ex.getProviderTransactionId());
        errorDetails.put("responseCode", ex.getResponseCode());
        errorDetails.put("reason", ex.getReason());
        errorDetails.put("message", ex.getMessage());

        ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                .code("PAYMENT_001")
                .message("PAYMENT_FAILED")
                .data(errorDetails)
                .build();

        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(response);
    }

    /**
     * Handle invalid payment callback exception
     */
    @ExceptionHandler(InvalidPaymentCallbackException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleInvalidCallback(InvalidPaymentCallbackException ex) {
        log.error("Invalid payment callback: {}", ex.getMessage());

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("transactionId", ex.getTransactionId());
        errorDetails.put("reason", ex.getReason());
        errorDetails.put("message", ex.getMessage());

        ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                .code("PAYMENT_002")
                .message("INVALID_PAYMENT_CALLBACK")
                .data(errorDetails)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle duplicate transaction exception
     */
    @ExceptionHandler(DuplicateTransactionException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleDuplicateTransaction(DuplicateTransactionException ex) {
        log.warn("Duplicate transaction: {}", ex.getMessage());

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("transactionId", ex.getTransactionId());
        errorDetails.put("providerTransactionId", ex.getProviderTransactionId());
        errorDetails.put("message", ex.getMessage());

        ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                .code("PAYMENT_003")
                .message("DUPLICATE_TRANSACTION")
                .data(errorDetails)
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

}

