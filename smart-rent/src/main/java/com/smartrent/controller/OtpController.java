package com.smartrent.controller;

import com.smartrent.dto.request.OtpSendRequest;
import com.smartrent.dto.request.OtpVerifyRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.OtpSendResponse;
import com.smartrent.dto.response.OtpVerifyResponse;
import com.smartrent.service.otp.OtpService;
import com.smartrent.service.otp.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for OTP phone verification operations
 *
 * Endpoints:
 * - POST /otp/send - Send OTP to phone number
 * - POST /otp/verify - Verify OTP code
 *
 * Security notes:
 * - These endpoints are typically public (no authentication required)
 * - Rate limiting is enforced per phone and per IP
 * - Consider adding CAPTCHA for production to prevent abuse
 * - For CSRF protection, use JWT tokens or configure CSRF appropriately
 */
@Slf4j
@RestController
@RequestMapping("/otp")
@RequiredArgsConstructor
@Tag(name = "OTP", description = "OTP phone verification API")
public class OtpController {

    private final OtpService otpService;
    private final RateLimitService rateLimitService;

    /**
     * Send OTP to phone number
     * 
     * Behavior:
     * 1. Normalize phone to E.164 format
     * 2. Reject non-Vietnam numbers
     * 3. Check rate limits (per phone and per IP)
     * 4. Attempt to send via Zalo ZNS first
     * 5. If Zalo fails, fallback to SMS
     * 6. Return channel used and request ID
     * 
     * @param request OTP send request
     * @param httpRequest HTTP request for IP extraction
     * @return OTP send response with channel, requestId, and TTL
     */
    @PostMapping("/send")
    @Operation(summary = "Send OTP", description = "Send OTP to Vietnam phone number via Zalo or SMS")
    public ResponseEntity<ApiResponse<OtpSendResponse>> sendOtp(
            @Valid @RequestBody OtpSendRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("Received OTP send request for phone: {}", maskPhone(request.getPhone()));
        
        // Extract client IP address
        String ipAddress = extractIpAddress(httpRequest);
        
        // Send OTP
        OtpSendResponse response = otpService.sendOtp(request, ipAddress);
        
        // Add rate limit headers
        int remainingPhoneAttempts = rateLimitService.getRemainingPhoneAttempts(request.getPhone());
        int remainingIpAttempts = rateLimitService.getRemainingIpAttempts(ipAddress);
        
        return ResponseEntity.ok()
            .header("X-RateLimit-Remaining-Phone", String.valueOf(remainingPhoneAttempts))
            .header("X-RateLimit-Remaining-IP", String.valueOf(remainingIpAttempts))
            .body(ApiResponse.<OtpSendResponse>builder()
                .code("0")
                .message("OTP sent successfully")
                .data(response)
                .build());
    }

    /**
     * Verify OTP code
     * 
     * Behavior:
     * 1. Retrieve stored OTP by phone and requestId
     * 2. Verify hashed OTP code
     * 3. Check verification attempts (max 5)
     * 4. Mark as verified and delete on success
     * 5. Increment attempts on failure
     * 
     * @param request OTP verify request
     * @return OTP verify response with verification result
     */
    @PostMapping("/verify")
    @Operation(summary = "Verify OTP", description = "Verify OTP code sent to phone number")
    public ResponseEntity<ApiResponse<OtpVerifyResponse>> verifyOtp(
            @Valid @RequestBody OtpVerifyRequest request) {
        
        log.info("Received OTP verify request for requestId: {}", request.getRequestId());
        
        // Verify OTP
        OtpVerifyResponse response = otpService.verifyOtp(request);
        
        HttpStatus status = response.getVerified() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        String code = response.getVerified() ? "0" : "10007";
        
        return ResponseEntity.status(status)
            .body(ApiResponse.<OtpVerifyResponse>builder()
                .code(code)
                .message(response.getMessage())
                .data(response)
                .build());
    }

    /**
     * Extract client IP address from request
     * Handles X-Forwarded-For header for proxied requests
     */
    private String extractIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("X-Real-IP");
        }
        
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        
        // If X-Forwarded-For contains multiple IPs, take the first one
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        
        return ipAddress;
    }

    /**
     * Mask phone number for logging
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 8) {
            return phone;
        }
        return phone.substring(0, Math.min(5, phone.length() - 4)) + "***";
    }
}

