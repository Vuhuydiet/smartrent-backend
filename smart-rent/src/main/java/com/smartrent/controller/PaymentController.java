package com.smartrent.controller;

import com.smartrent.dto.request.PaymentCallbackRequest;
import com.smartrent.dto.request.PaymentHistoryByStatusRequest;
import com.smartrent.dto.request.PaymentRefundRequest;
import com.smartrent.dto.request.PaymentRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.PaymentCallbackResponse;
import com.smartrent.dto.response.PaymentHistoryResponse;
import com.smartrent.dto.response.PaymentResponse;
import com.smartrent.dto.response.TransactionResponse;
import com.smartrent.enums.PaymentProvider;
import com.smartrent.enums.TransactionStatus;
import com.smartrent.enums.TransactionType;
import com.smartrent.infra.repository.entity.Transaction;
import com.smartrent.service.listing.ListingService;
import com.smartrent.service.membership.MembershipService;
import com.smartrent.service.payment.PaymentService;
import com.smartrent.service.push.PushService;
import com.smartrent.service.transaction.TransactionService;
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
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Payment", description = "Payment management APIs")
public class PaymentController {

    PaymentService paymentService;
    TransactionService transactionService;
    MembershipService membershipService;
    ListingService listingService;
    PushService pushService;

    // Generic Payment Endpoints (Provider-agnostic)

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

            // If payment was successful, trigger business logic completion
            if (response.getSignatureValid() && response.getSuccess() && response.getTransactionRef() != null) {
                try {
                    triggerBusinessLogicCompletion(response.getTransactionRef());
                } catch (Exception e) {
                    log.error("Error triggering business logic completion for transaction: {}",
                            response.getTransactionRef(), e);
                    // Don't fail the IPN response - transaction is already updated
                }
            }

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
    @Operation(
        summary = "Get payment history",
        description = "Get user payment history with pagination (1-based page indexing)",
        parameters = {
            @io.swagger.v3.oas.annotations.Parameter(
                name = "userId",
                description = "User ID (UUID format)",
                example = "13ad9071-279a-4758-9caf-9758d259187d",
                required = true
            ),
            @io.swagger.v3.oas.annotations.Parameter(
                name = "page",
                description = "Page number (1-based indexing)",
                example = "1"
            ),
            @io.swagger.v3.oas.annotations.Parameter(
                name = "size",
                description = "Number of items per page",
                example = "20"
            )
        }
    )
    public ApiResponse<Page<PaymentHistoryResponse>> getPaymentHistory(
            @RequestParam String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Getting payment history for user: {} - page: {}, size: {}", userId, page, size);

        try {
            Pageable pageable = PageRequest.of(page - 1, size);
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
    @Operation(
        summary = "Get payment history by status",
        description = "Get user payment history filtered by transaction status with pagination (1-based page indexing)",
        parameters = {
            @io.swagger.v3.oas.annotations.Parameter(
                name = "userId",
                description = "User ID (UUID format)",
                example = "13ad9071-279a-4758-9caf-9758d259187d",
                required = true
            ),
            @io.swagger.v3.oas.annotations.Parameter(
                name = "status",
                description = "Transaction status to filter by",
                example = "COMPLETED",
                required = true
            ),
            @io.swagger.v3.oas.annotations.Parameter(
                name = "page",
                description = "Page number (1-based indexing)",
                example = "1"
            ),
            @io.swagger.v3.oas.annotations.Parameter(
                name = "size",
                description = "Number of items per page",
                example = "20"
            )
        }
    )
    public ApiResponse<Page<PaymentHistoryResponse>> getPaymentHistoryByStatus(
            @RequestParam String userId,
            @PathVariable TransactionStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Getting payment history for user: {} with status: {} - page: {}, size: {}", userId, status, page, size);

        try {
            Pageable pageable = PageRequest.of(page - 1, size);
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

    @GetMapping("/transactions/{txnRef}")
    @Operation(summary = "Query transaction", description = "Query transaction status and details by transaction reference")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Transaction found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Transaction not found"
            )
    })
    public ApiResponse<TransactionResponse> queryTransaction(
            @Parameter(description = "Transaction reference") @PathVariable String txnRef) {

        log.info("Querying transaction: {}", txnRef);

        try {
            TransactionResponse response = transactionService.getTransactionResponse(txnRef);

            return ApiResponse.<TransactionResponse>builder()
                    .code("200000")
                    .message("Transaction queried successfully")
                    .data(response)
                    .build();

        } catch (Exception e) {
            log.error("Error querying transaction: {}", txnRef, e);
            return ApiResponse.<TransactionResponse>builder()
                    .code("404000")
                    .message("Transaction not found: " + e.getMessage())
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

    // Private helper methods

    /**
     * Trigger business logic completion based on transaction type
     * Called after successful payment IPN
     */
    private void triggerBusinessLogicCompletion(String transactionRef) {
        log.info("Triggering business logic completion for transaction: {}", transactionRef);

        try {
            // Get transaction to determine type
            Transaction transaction = transactionService.getTransaction(transactionRef);

            if (transaction == null) {
                log.warn("Transaction not found: {}", transactionRef);
                return;
            }

            // Check if already processed
            if (transaction.getStatus() != TransactionStatus.COMPLETED) {
                log.warn("Transaction not completed, skipping business logic: {}", transactionRef);
                return;
            }

            // Trigger appropriate business logic based on transaction type
            TransactionType type = transaction.getTransactionType();
            log.info("Processing transaction type: {} for transaction: {}", type, transactionRef);

            switch (type) {
                case MEMBERSHIP_PURCHASE -> {
                    log.info("Completing membership purchase for transaction: {}", transactionRef);
                    membershipService.completeMembershipPurchase(transactionRef);
                }
                case POST_FEE -> {
                    log.info("Completing listing creation (NORMAL or VIP) for transaction: {}", transactionRef);
                    listingService.completeListingCreationAfterPayment(transactionRef);
                }
                case PUSH_FEE -> {
                    log.info("Completing push after payment for transaction: {}", transactionRef);
                    pushService.completePushAfterPayment(transactionRef);
                }
                default -> log.warn("Unknown transaction type: {} for transaction: {}", type, transactionRef);
            }

            log.info("Business logic completion triggered successfully for transaction: {}", transactionRef);

        } catch (Exception e) {
            log.error("Error triggering business logic completion for transaction: {}", transactionRef, e);
            throw new RuntimeException("Failed to trigger business logic completion", e);
        }
    }

}
