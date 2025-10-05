package com.smartrent.controller;

import com.smartrent.dto.request.PaymentCallbackRequest;
import com.smartrent.dto.request.PaymentHistoryByStatusRequest;
import com.smartrent.dto.request.PaymentRefundRequest;
import com.smartrent.dto.request.PaymentRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.PaymentCallbackResponse;
import com.smartrent.dto.response.PaymentHistoryResponse;
import com.smartrent.dto.response.PaymentResponse;
import com.smartrent.dto.response.CreditBalanceResponse;
import com.smartrent.enums.PaymentProvider;
import com.smartrent.enums.TransactionStatus;
import com.smartrent.service.payment.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Payment", description = "Payment management APIs")
public class PaymentController {

    PaymentService paymentService;

    // Generic Payment Endpoints (Provider-agnostic)

    @PostMapping("/create")
    @Operation(summary = "Create payment", description = "Create a new payment with any supported provider")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ApiResponse<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentRequest request,
            HttpServletRequest httpRequest) {

        log.info("Creating payment for provider: {} with amount: {}", request.getProvider(), request.getAmount());

        try {
            PaymentResponse response = paymentService.createPayment(request, httpRequest);

            return ApiResponse.<PaymentResponse>builder()
                    .code("200000")
                    .message("Payment created successfully")
                    .data(response)
                    .build();

        } catch (Exception e) {
            log.error("Error creating payment", e);
            return ApiResponse.<PaymentResponse>builder()
                    .code("500000")
                    .message("Failed to create payment: " + e.getMessage())
                    .build();
        }
    }

    @GetMapping("/callback/{provider}")
    @Operation(summary = "Payment callback", description = "Handle payment callback from any provider")
    public void handlePaymentCallback(
            @Parameter(description = "Payment provider") @PathVariable PaymentProvider provider,
            @Parameter(description = "Callback parameters") @RequestParam Map<String, String> params,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) throws IOException {

        log.info("Handling callback for provider: {}", provider);

        try {
            PaymentCallbackRequest callbackRequest = PaymentCallbackRequest.builder()
                    .provider(provider)
                    .params(params)
                    .build();
            PaymentCallbackResponse response = paymentService.processCallback(callbackRequest, httpRequest);

            // Redirect to frontend with payment result
            String redirectUrl = buildRedirectUrl(response);
            httpResponse.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("Error processing callback for provider: {}", provider, e);
            httpResponse.sendRedirect("/payment/error?message=" + e.getMessage());
        }
    }

    @PostMapping("/ipn/{provider}")
    @Operation(summary = "Payment IPN endpoint", description = "Handle Instant Payment Notification from any provider")
    public ResponseEntity<String> handlePaymentIPN(
            @Parameter(description = "Payment provider") @PathVariable PaymentProvider provider,
            @RequestParam Map<String, String> params,
            HttpServletRequest httpRequest) {

        log.info("Handling IPN for provider: {}", provider);

        try {
            PaymentCallbackRequest ipnRequest = PaymentCallbackRequest.builder()
                    .provider(provider)
                    .params(params)
                    .build();
            PaymentCallbackResponse response = paymentService.processIPN(ipnRequest, httpRequest);

            if (response.getSignatureValid() && response.getSuccess()) {
                return ResponseEntity.ok("RspCode=00&Message=OK");
            } else {
                return ResponseEntity.ok("RspCode=01&Message=FAIL");
            }

        } catch (Exception e) {
            log.error("Error processing IPN for provider: {}", provider, e);
            return ResponseEntity.ok("RspCode=99&Message=ERROR");
        }
    }

    @PostMapping("/refund/{transactionRef}")
    @Operation(summary = "Refund payment", description = "Refund a payment")
    public ApiResponse<PaymentCallbackResponse> refundPayment(
            @Parameter(description = "Transaction reference") @PathVariable String transactionRef,
            @Parameter(description = "Refund amount") @RequestParam String amount,
            @Parameter(description = "Refund reason") @RequestParam String reason) {

        log.info("Refunding payment: {} with amount: {}", transactionRef, amount);

        try {
            PaymentRefundRequest refundRequest = PaymentRefundRequest.builder()
                    .transactionRef(transactionRef)
                    .amount(amount)
                    .reason(reason)
                    .build();
            PaymentCallbackResponse response = paymentService.refundPayment(refundRequest);

            return ApiResponse.<PaymentCallbackResponse>builder()
                    .code("200000")
                    .message("Payment refunded successfully")
                    .data(response)
                    .build();

        } catch (Exception e) {
            log.error("Error refunding payment: {}", transactionRef, e);
            return ApiResponse.<PaymentCallbackResponse>builder()
                    .code("500000")
                    .message("Failed to refund payment: " + e.getMessage())
                    .build();
        }
    }

    @GetMapping("/providers")
    @Operation(summary = "Get available payment providers", description = "Get list of available payment providers")
    public ApiResponse<List<PaymentProvider>> getAvailableProviders() {
        try {
            List<PaymentProvider> providers = paymentService.getAvailableProviders();

            return ApiResponse.<List<PaymentProvider>>builder()
                    .code("200000")
                    .message("Available providers retrieved successfully")
                    .data(providers)
                    .build();

        } catch (Exception e) {
            log.error("Error getting available providers", e);
            return ApiResponse.<List<PaymentProvider>>builder()
                    .code("500000")
                    .message("Failed to get providers: " + e.getMessage())
                    .build();
        }
    }

    @GetMapping("/history")
    @Operation(summary = "Get payment history", description = "Get user payment history with pagination")
    public ApiResponse<Page<PaymentHistoryResponse>> getPaymentHistory(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        log.info("Getting payment history for user: {}", userId);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<PaymentHistoryResponse> response = paymentService.getPaymentHistory(userId, pageable);

            return ApiResponse.<Page<PaymentHistoryResponse>>builder()
                    .code("200000")
                    .message("Payment history retrieved successfully")
                    .data(response)
                    .build();

        } catch (Exception e) {
            log.error("Error getting payment history for user: {}", userId, e);
            return ApiResponse.<Page<PaymentHistoryResponse>>builder()
                    .code("500000")
                    .message("Failed to get payment history: " + e.getMessage())
                    .build();
        }
    }

    @GetMapping("/history/status/{status}")
    @Operation(summary = "Get payment history by status", description = "Get user payment history filtered by status")
    public ApiResponse<Page<PaymentHistoryResponse>> getPaymentHistoryByStatus(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Transaction status") @PathVariable TransactionStatus status,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        log.info("Getting payment history for user: {} with status: {}", userId, status);

        try {
            Pageable pageable = PageRequest.of(page, size);
            PaymentHistoryByStatusRequest historyRequest = PaymentHistoryByStatusRequest.builder()
                    .userId(userId)
                    .status(status)
                    .pageable(pageable)
                    .build();
            Page<PaymentHistoryResponse> response = paymentService.getPaymentHistoryByStatus(historyRequest);

            return ApiResponse.<Page<PaymentHistoryResponse>>builder()
                    .code("200000")
                    .message("Payment history retrieved successfully")
                    .data(response)
                    .build();

        } catch (Exception e) {
            log.error("Error getting payment history for user: {} with status: {}", userId, status, e);
            return ApiResponse.<Page<PaymentHistoryResponse>>builder()
                    .code("500000")
                    .message("Failed to get payment history: " + e.getMessage())
                    .build();
        }
    }

    @PostMapping("/cancel/{transactionRef}")
    @Operation(summary = "Cancel payment", description = "Cancel a pending payment")
    public ApiResponse<Boolean> cancelPayment(
            @Parameter(description = "Transaction reference") @PathVariable String transactionRef,
            @Parameter(description = "Cancellation reason") @RequestParam String reason) {

        log.info("Cancelling payment: {} with reason: {}", transactionRef, reason);

        try {
            boolean cancelled = paymentService.cancelPayment(transactionRef, reason);

            if (cancelled) {
                return ApiResponse.<Boolean>builder()
                        .code("200000")
                        .message("Payment cancelled successfully")
                        .data(true)
                        .build();
            } else {
                return ApiResponse.<Boolean>builder()
                        .code("400000")
                        .message("Failed to cancel payment")
                        .data(false)
                        .build();
            }

        } catch (Exception e) {
            log.error("Error cancelling payment: {}", transactionRef, e);
            return ApiResponse.<Boolean>builder()
                    .code("500000")
                    .message("Failed to cancel payment: " + e.getMessage())
                    .data(false)
                    .build();
        }
    }

    @GetMapping("/exists/{transactionRef}")
    @Operation(summary = "Check transaction existence", description = "Check if transaction reference exists")
    public ApiResponse<Boolean> checkTransactionExists(
            @Parameter(description = "Transaction reference") @PathVariable String transactionRef) {

        try {
            boolean exists = paymentService.transactionRefExists(transactionRef);

            return ApiResponse.<Boolean>builder()
                    .code("200000")
                    .message("Transaction existence checked")
                    .data(exists)
                    .build();

        } catch (Exception e) {
            log.error("Error checking transaction existence: {}", transactionRef, e);
            return ApiResponse.<Boolean>builder()
                    .code("500000")
                    .message("Failed to check transaction: " + e.getMessage())
                    .data(false)
                    .build();
        }
    }

    // Wallet/Credit Management Endpoints

    @GetMapping("/wallet/balance")
    @Operation(
            summary = "Get user wallet balance",
            description = "Get current credit balance and wallet information for the authenticated user or any user (admin only)",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Balance retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing authentication token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Cannot access other user's wallet"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("#userId.toString() == #authenticatedUserId or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ApiResponse<CreditBalanceResponse> getUserWalletBalance(
            @Parameter(
                    name = "X-User-Id",
                    description = "The authenticated user ID from JWT token",
                    required = true,
                    example = "user-123e4567-e89b-12d3-a456-426614174000"
            )
            @RequestHeader(Constants.USER_ID) String authenticatedUserId,
            @Parameter(description = "User ID to get balance for (defaults to authenticated user)")
            @RequestParam(required = false) Long userId) {

        // If userId is not provided, use the authenticated user's ID
        Long targetUserId = userId != null ? userId : Long.valueOf(authenticatedUserId);

        log.info("Getting wallet balance for user: {} (requested by: {})", targetUserId, authenticatedUserId);

        try {
            CreditBalanceResponse response = paymentService.getUserCreditBalance(targetUserId);

            return ApiResponse.<CreditBalanceResponse>builder()
                    .code("200000")
                    .message("Wallet balance retrieved successfully")
                    .data(response)
                    .build();

        } catch (Exception e) {
            log.error("Error getting wallet balance for user: {} (requested by: {})", targetUserId, authenticatedUserId, e);
            return ApiResponse.<CreditBalanceResponse>builder()
                    .code("500000")
                    .message("Failed to get wallet balance: " + e.getMessage())
                    .build();
        }
    }

    @GetMapping("/wallet/transactions")
    @Operation(
            summary = "Get wallet transaction history",
            description = "Get credit transaction history for the authenticated user or any user (admin only) with pagination",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction history retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing authentication token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Cannot access other user's transactions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("#userId == null or #userId.toString() == #authenticatedUserId or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ApiResponse<Page<PaymentHistoryResponse>> getWalletTransactionHistory(
            @Parameter(
                    name = "X-User-Id",
                    description = "The authenticated user ID from JWT token",
                    required = true,
                    example = "user-123e4567-e89b-12d3-a456-426614174000"
            )
            @RequestHeader(Constants.USER_ID) String authenticatedUserId,
            @Parameter(description = "User ID to get transactions for (defaults to authenticated user)")
            @RequestParam(required = false) Long userId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        // If userId is not provided, use the authenticated user's ID
        Long targetUserId = userId != null ? userId : Long.valueOf(authenticatedUserId);

        log.info("Getting wallet transaction history for user: {} (requested by: {})", targetUserId, authenticatedUserId);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<PaymentHistoryResponse> response = paymentService.getCreditTransactionHistory(targetUserId, pageable);

            return ApiResponse.<Page<PaymentHistoryResponse>>builder()
                    .code("200000")
                    .message("Wallet transaction history retrieved successfully")
                    .data(response)
                    .build();

        } catch (Exception e) {
            log.error("Error getting wallet transaction history for user: {} (requested by: {})", targetUserId, authenticatedUserId, e);
            return ApiResponse.<Page<PaymentHistoryResponse>>builder()
                    .code("500000")
                    .message("Failed to get wallet transaction history: " + e.getMessage())
                    .build();
        }
    }

    // Private helper methods

    private String buildRedirectUrl(PaymentCallbackResponse response) {
        // Build redirect URL based on payment result
        // This should point to your frontend application
        String baseUrl = "http://localhost:3000/payment/result"; // Configure this

        StringBuilder url = new StringBuilder(baseUrl);
        url.append("?transactionRef=").append(response.getTransactionRef());
        url.append("&success=").append(response.getSuccess());
        url.append("&status=").append(response.getStatus());
        url.append("&provider=").append(response.getProvider());

        if (response.getAmount() != null) {
            url.append("&amount=").append(response.getAmount());
        }

        if (response.getResponseMessage() != null) {
            url.append("&message=").append(response.getResponseMessage());
        }

        return url.toString();
    }

}
