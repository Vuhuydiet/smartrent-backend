package com.smartrent.service.otp.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrent.config.otp.ZaloProperties;
import com.smartrent.enums.OtpChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Zalo ZNS (Zalo Notification Service) provider for OTP delivery
 * 
 * IMPORTANT: Zalo requires:
 * 1. Official Account (OA) registration and approval
 * 2. Template registration with Zalo - template must contain an OTP placeholder
 * 3. Access token from Zalo Open API
 * 4. Template ID for OTP messages
 * 
 * See: https://developers.zalo.me/docs/zalo-notification-service
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "otp.providers.zalo.enabled", havingValue = "true")
public class ZaloZnsProvider implements OtpProvider {

    private final ZaloProperties zaloProperties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public ZaloZnsProvider(ZaloProperties zaloProperties, WebClient.Builder webClientBuilder) {
        this.zaloProperties = zaloProperties;
        this.webClient = webClientBuilder
            .baseUrl(zaloProperties.getApiEndpoint())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public OtpChannel getChannel() {
        return OtpChannel.ZALO;
    }

    @Override
    public OtpProviderResult sendOtp(String phone, String otpCode, Map<String, Object> context) {
        log.info("Sending OTP via Zalo ZNS to phone: {}", maskPhone(phone));

        try {
            // Build request payload
            Map<String, Object> payload = buildZaloPayload(phone, otpCode, context);
            
            // Send request with retry logic
            String response = webClient.post()
                .header("access_token", zaloProperties.getAccessToken())
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(
                        zaloProperties.getMaxRetryAttempts(),
                        Duration.ofSeconds(zaloProperties.getRetryBackoffSeconds())
                    )
                    .filter(this::isRetryableError)
                    .doBeforeRetry(retrySignal ->
                        log.warn("Retrying Zalo ZNS request, attempt: {}", retrySignal.totalRetries() + 1)
                    )
                )
                .timeout(Duration.ofSeconds(zaloProperties.getRequestTimeoutSeconds()))
                .block();

            // Parse response
            return parseZaloResponse(response);

        } catch (WebClientResponseException e) {
            log.error("Zalo ZNS API error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            return handleZaloError(e);
        } catch (Exception e) {
            log.error("Failed to send OTP via Zalo ZNS", e);
            return OtpProviderResult.failure("ZALO_ERROR", e.getMessage(), true);
        }
    }

    @Override
    public boolean isAvailable() {
        return zaloProperties.getAccessToken() != null && 
               !zaloProperties.getAccessToken().isEmpty() &&
               zaloProperties.getOaId() != null &&
               zaloProperties.getTemplateId() != null;
    }

    @Override
    public String getProviderName() {
        return "Zalo ZNS";
    }

    /**
     * Build Zalo ZNS API payload
     * Template data structure depends on your registered template
     */
    private Map<String, Object> buildZaloPayload(String phone, String otpCode, Map<String, Object> context) {
        Map<String, Object> payload = new HashMap<>();
        
        // Remove country code prefix for Zalo (expects 84xxxxxxxxx format without +)
        String zaloPhone = phone.replace("+", "");
        
        payload.put("phone", zaloPhone);
        payload.put("template_id", zaloProperties.getTemplateId());
        payload.put("template_data", buildTemplateData(otpCode, context));
        
        // Optional: tracking ID
        if (context != null && context.containsKey("requestId")) {
            payload.put("tracking_id", context.get("requestId"));
        }

        return payload;
    }

    /**
     * Build template data based on your Zalo template structure
     * Example template might have fields like: otp_code, expire_time, app_name
     */
    private Map<String, String> buildTemplateData(String otpCode, Map<String, Object> context) {
        Map<String, String> templateData = new HashMap<>();

        // Map OTP code to template field (adjust field name based on your template)
        templateData.put("otp_code", otpCode);

        // Add expiration time if provided
        if (context != null && context.containsKey("expiryMinutes")) {
            templateData.put("expire_time", context.get("expiryMinutes").toString() + " phút");
        } else {
            templateData.put("expire_time", zaloProperties.getDefaultExpiryMinutes() + " phút");
        }

        // Add app name from configuration
        templateData.put("app_name", zaloProperties.getAppName());

        return templateData;
    }

    /**
     * Parse Zalo API response
     * Zalo returns JSON with structure: {"error": 0, "message": "Success", "data": {...}}
     */
    private OtpProviderResult parseZaloResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            int errorCode = root.path("error").asInt();
            String message = root.path("message").asText();
            
            if (errorCode == 0) {
                // Success
                String messageId = root.path("data").path("msg_id").asText();
                log.info("Zalo ZNS OTP sent successfully, msg_id: {}", messageId);
                return OtpProviderResult.success(messageId);
            } else {
                // Zalo error
                log.warn("Zalo ZNS returned error: code={}, message={}", errorCode, message);
                boolean retryable = isZaloErrorRetryable(errorCode);
                return OtpProviderResult.failure("ZALO_" + errorCode, message, retryable);
            }
        } catch (Exception e) {
            log.error("Failed to parse Zalo response", e);
            return OtpProviderResult.failure("ZALO_PARSE_ERROR", "Failed to parse response", false);
        }
    }

    /**
     * Handle Zalo HTTP errors
     */
    private OtpProviderResult handleZaloError(WebClientResponseException e) {
        HttpStatus status = (HttpStatus) e.getStatusCode();
        boolean retryable = status.is5xxServerError() || status == HttpStatus.TOO_MANY_REQUESTS;
        
        return OtpProviderResult.failure(
            "ZALO_HTTP_" + status.value(),
            "Zalo API error: " + e.getMessage(),
            retryable
        );
    }

    /**
     * Check if error is retryable
     */
    private boolean isRetryableError(Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            HttpStatus status = (HttpStatus) ((WebClientResponseException) throwable).getStatusCode();
            return status.is5xxServerError() || status == HttpStatus.TOO_MANY_REQUESTS;
        }
        return false;
    }

    /**
     * Check if Zalo error code is retryable
     * Common Zalo error codes:
     * -124: Invalid access token (not retryable)
     * -214: Invalid phone number (not retryable)
     * -216: Template not found (not retryable)
     * -217: OA not approved (not retryable)
     */
    private boolean isZaloErrorRetryable(int errorCode) {
        return errorCode != -124 && errorCode != -214 && errorCode != -216 && errorCode != -217;
    }

    /**
     * Mask phone number for logging
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 8) {
            return phone;
        }
        return phone.substring(0, 5) + "***" + phone.substring(phone.length() - 3);
    }
}

