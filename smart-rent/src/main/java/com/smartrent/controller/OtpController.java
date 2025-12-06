package com.smartrent.controller;

import com.smartrent.dto.request.OtpSendRequest;
import com.smartrent.dto.request.OtpVerifyRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.OtpSendResponse;
import com.smartrent.dto.response.OtpVerifyResponse;
import com.smartrent.service.otp.OtpService;
import com.smartrent.service.otp.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@Tag(name = "OTP", description = """
        APIs for OTP (One-Time Password) phone verification.

        **Features:**
        - Send OTP to Vietnam phone numbers via Zalo ZNS or SMS
        - Verify OTP codes for phone number validation
        - Rate limiting per phone number and IP address

        **Flow:**
        1. User requests OTP via POST /otp/send
        2. System sends OTP via Zalo ZNS (primary) or SMS (fallback)
        3. User receives OTP and submits via POST /otp/verify
        4. System validates OTP and marks phone as verified

        **Rate Limits:**
        - Per phone: Limited requests per time window
        - Per IP: Limited requests per time window
        - Rate limit headers included in response
        """)
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
    @Operation(
            summary = "Send OTP to phone number",
            description = """
                    Send OTP to a Vietnam phone number via Zalo ZNS or SMS.

                    **Behavior:**
                    1. Normalize phone to E.164 format
                    2. Reject non-Vietnam numbers
                    3. Check rate limits (per phone and per IP)
                    4. Attempt to send via Zalo ZNS first
                    5. If Zalo fails, fallback to SMS
                    6. Return channel used and request ID

                    **Rate Limit Headers:**
                    - X-RateLimit-Remaining-Phone: Remaining attempts for this phone
                    - X-RateLimit-Remaining-IP: Remaining attempts for this IP
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "OTP send request with phone number",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OtpSendRequest.class),
                            examples = @ExampleObject(
                                    name = "Send OTP Example",
                                    value = """
                                            {
                                              "phone": "0912345678"
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OTP sent successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": "OTP sent successfully",
                                              "data": {
                                                "requestId": "req-123e4567-e89b-12d3-a456-426614174000",
                                                "channel": "ZALO",
                                                "ttlSeconds": 300,
                                                "phone": "0912345678"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid phone number format or non-Vietnam number",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Invalid Phone Error",
                                    value = """
                                            {
                                              "code": "400001",
                                              "message": "Invalid phone number format",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "429",
                    description = "Rate limit exceeded",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Rate Limit Error",
                                    value = """
                                            {
                                              "code": "429001",
                                              "message": "Too many OTP requests. Please try again later.",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Failed to send OTP",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Server Error",
                                    value = """
                                            {
                                              "code": "500001",
                                              "message": "Failed to send OTP. Please try again.",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
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
    @Operation(
            summary = "Verify OTP code",
            description = """
                    Verify OTP code sent to phone number.

                    **Behavior:**
                    1. Retrieve stored OTP by phone and requestId
                    2. Verify hashed OTP code
                    3. Check verification attempts (max 5)
                    4. Mark as verified and delete on success
                    5. Increment attempts on failure

                    **Returns:**
                    - verified: true/false indicating verification result
                    - message: Description of the result
                    - remainingAttempts: Number of remaining verification attempts
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "OTP verification request",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OtpVerifyRequest.class),
                            examples = @ExampleObject(
                                    name = "Verify OTP Example",
                                    value = """
                                            {
                                              "phone": "0912345678",
                                              "requestId": "req-123e4567-e89b-12d3-a456-426614174000",
                                              "otp": "123456"
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OTP verified successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": "OTP verified successfully",
                                              "data": {
                                                "verified": true,
                                                "message": "Phone number verified successfully",
                                                "remainingAttempts": 0
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid OTP code or verification failed",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "Invalid OTP",
                                            value = """
                                                    {
                                                      "code": "400001",
                                                      "message": "Invalid OTP code",
                                                      "data": {
                                                        "verified": false,
                                                        "message": "Invalid OTP code",
                                                        "remainingAttempts": 4
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "OTP Expired",
                                            value = """
                                                    {
                                                      "code": "400002",
                                                      "message": "OTP has expired",
                                                      "data": {
                                                        "verified": false,
                                                        "message": "OTP has expired. Please request a new one.",
                                                        "remainingAttempts": 0
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Max Attempts Exceeded",
                                            value = """
                                                    {
                                                      "code": "400003",
                                                      "message": "Maximum verification attempts exceeded",
                                                      "data": {
                                                        "verified": false,
                                                        "message": "Maximum verification attempts exceeded. Please request a new OTP.",
                                                        "remainingAttempts": 0
                                                      }
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "OTP request not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Not Found Error",
                                    value = """
                                            {
                                              "code": "404001",
                                              "message": "OTP request not found",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
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

