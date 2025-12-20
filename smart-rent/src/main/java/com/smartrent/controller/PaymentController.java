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
@Tag(name = "Payment", description = """
        APIs for payment management and transaction processing.

        **Features:**
        - Process payments via multiple providers (VNPay, etc.)
        - Handle Instant Payment Notifications (IPN)
        - Query transaction status and history
        - Refund and cancel payments

        **Payment Flow:**
        1. User initiates payment (via membership, listing, or push APIs)
        2. System creates transaction and returns payment URL
        3. User completes payment on provider's page
        4. Provider sends IPN to /ipn/{provider}
        5. System processes IPN and triggers business logic
        6. Frontend can verify status via /transactions/{txnRef}

        **Transaction Types:**
        - MEMBERSHIP_PURCHASE: Membership package purchase
        - POST_FEE: Listing creation fee (Normal or VIP)
        - PUSH_FEE: Push listing fee
        """)
public class PaymentController {

    PaymentService paymentService;
    TransactionService transactionService;
    MembershipService membershipService;
    ListingService listingService;
    PushService pushService;

    // Generic Payment Endpoints (Provider-agnostic)

    @GetMapping("/callback/{provider}")
    @Operation(
            summary = "Payment callback endpoint (for frontend)",
            description = """
                    Process payment result after user is redirected back from payment provider.

                    **This is a PUBLIC endpoint - no authentication required.**

                    **Use Case:**
                    After user completes payment on VNPAY, they are redirected to frontend with query parameters.
                    Frontend should call this endpoint with those same parameters to update transaction status.

                    **Flow:**
                    1. User pays on VNPAY
                    2. VNPAY redirects to: `{frontend}/payment/result?vnp_TxnRef=xxx&vnp_ResponseCode=00&...`
                    3. Frontend calls: `GET /v1/payments/callback/VNPAY?vnp_TxnRef=xxx&vnp_ResponseCode=00&...`
                    4. Backend validates signature, updates transaction, triggers business logic
                    5. Frontend displays result to user

                    **Note:** This achieves the same result as IPN but is initiated by frontend instead of VNPAY server.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Payment callback processed",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApiResponse.class),
                            examples = {
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "Success",
                                            value = """
                                                    {
                                                      "code": "200000",
                                                      "message": "Payment completed successfully",
                                                      "data": {
                                                        "transactionRef": "d1171b46-02ce-4d68-98b5-f1f3aeca9c5a",
                                                        "providerTransactionId": "15356501",
                                                        "status": "COMPLETED",
                                                        "success": true,
                                                        "signatureValid": true,
                                                        "message": "Payment successful"
                                                      }
                                                    }
                                                    """
                                    ),
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "Failed Payment",
                                            value = """
                                                    {
                                                      "code": "400000",
                                                      "message": "Payment failed",
                                                      "data": {
                                                        "transactionRef": "d1171b46-02ce-4d68-98b5-f1f3aeca9c5a",
                                                        "status": "FAILED",
                                                        "success": false,
                                                        "signatureValid": true,
                                                        "message": "User cancelled payment"
                                                      }
                                                    }
                                                    """
                                    ),
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "Invalid Signature",
                                            value = """
                                                    {
                                                      "code": "400001",
                                                      "message": "Invalid signature",
                                                      "data": {
                                                        "success": false,
                                                        "signatureValid": false,
                                                        "message": "Signature validation failed"
                                                      }
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    })
    public ApiResponse<PaymentCallbackResponse> handlePaymentCallback(
            @Parameter(description = "Payment provider (e.g., VNPAY)", example = "VNPAY") @PathVariable PaymentProvider provider,
            @Parameter(description = "All query parameters from payment provider redirect") @RequestParam Map<String, String> params,
            HttpServletRequest httpRequest) {

        log.info("Processing payment callback for provider: {} with params: {}", provider, params.keySet());

        try {
            PaymentCallbackRequest callbackRequest = PaymentCallbackRequest.builder()
                    .provider(provider)
                    .params(params)
                    .build();
            PaymentCallbackResponse response = paymentService.processIPN(callbackRequest, httpRequest);

            // If payment was successful, trigger business logic completion
            if (response.getSignatureValid() && response.getSuccess() && response.getTransactionRef() != null) {
                try {
                    triggerBusinessLogicCompletion(response.getTransactionRef());
                    log.info("Business logic triggered for transaction: {}", response.getTransactionRef());
                } catch (Exception e) {
                    log.error("Error triggering business logic completion for transaction: {}",
                            response.getTransactionRef(), e);
                    // Don't fail the response - transaction is already updated
                }
            }

            // Return appropriate response based on result
            if (!response.getSignatureValid()) {
                return ApiResponse.<PaymentCallbackResponse>builder()
                        .code("400001")
                        .message("Invalid signature")
                        .data(response)
                        .build();
            } else if (response.getSuccess()) {
                return ApiResponse.<PaymentCallbackResponse>builder()
                        .code("200000")
                        .message("Payment completed successfully")
                        .data(response)
                        .build();
            } else {
                return ApiResponse.<PaymentCallbackResponse>builder()
                        .code("400000")
                        .message("Payment failed: " + response.getMessage())
                        .data(response)
                        .build();
            }

        } catch (Exception e) {
            log.error("Error processing payment callback for provider: {}", provider, e);
            return ApiResponse.<PaymentCallbackResponse>builder()
                    .code("500000")
                    .message("Error processing payment callback: " + e.getMessage())
                    .build();
        }
    }

    @PostMapping("/refund/{transactionRef}")
    @Operation(
            summary = "Refund payment",
            description = """
                    Refund a completed payment.

                    **Requirements:**
                    - Transaction must exist and be in COMPLETED status
                    - Refund amount must not exceed original amount
                    - Admin authentication required

                    **Note:** Partial refunds may be supported depending on the payment provider.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Payment refunded successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": "Payment refunded successfully",
                                              "data": {
                                                "transactionRef": "TXN123456",
                                                "success": true,
                                                "signatureValid": true
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Transaction not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid refund request (e.g., amount exceeds original)"
            )
    })
    public ApiResponse<PaymentCallbackResponse> refundPayment(
            @Parameter(description = "Transaction reference", example = "TXN123456") @PathVariable String transactionRef,
            @Parameter(description = "Refund amount in VND", example = "100000") @RequestParam String amount,
            @Parameter(description = "Refund reason", example = "Customer request") @RequestParam String reason) {

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
    @Operation(
            summary = "Get available payment providers",
            description = "Get list of available payment providers configured in the system"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Providers retrieved successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": "Available providers retrieved successfully",
                                              "data": ["VNPAY"]
                                            }
                                            """
                            )
                    )
            )
    })
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
    @Operation(
            summary = "Cancel payment",
            description = """
                    Cancel a pending payment.

                    **Requirements:**
                    - Transaction must exist and be in PENDING status
                    - Only the user who created the transaction can cancel it

                    **Note:** Completed transactions cannot be cancelled. Use refund instead.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Payment cancelled successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": "Payment cancelled successfully",
                                              "data": true
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Cannot cancel payment (e.g., already completed)",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Error Response",
                                    value = """
                                            {
                                              "code": "400001",
                                              "message": "Failed to cancel payment",
                                              "data": false
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Transaction not found"
            )
    })
    public ApiResponse<Boolean> cancelPayment(
            @Parameter(description = "Transaction reference", example = "TXN123456") @PathVariable String transactionRef,
            @Parameter(description = "Cancellation reason", example = "User changed mind") @RequestParam String reason) {

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
    @Operation(
            summary = "Query transaction",
            description = """
                    Query transaction status and details by transaction reference.

                    **Use Cases:**
                    - Frontend polling after payment redirect
                    - Checking payment status before proceeding
                    - Displaying transaction details to user

                    **Returns:**
                    - Transaction status (PENDING, COMPLETED, FAILED, CANCELLED, REFUNDED)
                    - Transaction type (MEMBERSHIP_PURCHASE, POST_FEE, PUSH_FEE)
                    - Amount and currency
                    - Created and updated timestamps
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Transaction found",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": "Transaction queried successfully",
                                              "data": {
                                                "transactionRef": "TXN123456",
                                                "status": "COMPLETED",
                                                "type": "MEMBERSHIP_PURCHASE",
                                                "amount": 100000,
                                                "currency": "VND",
                                                "provider": "VNPAY",
                                                "createdAt": "2024-01-15T10:30:00",
                                                "updatedAt": "2024-01-15T10:35:00"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Transaction not found",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Not Found Response",
                                    value = """
                                            {
                                              "code": "404001",
                                              "message": "Transaction not found",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    public ApiResponse<TransactionResponse> queryTransaction(
            @Parameter(description = "Transaction reference", example = "TXN123456") @PathVariable String txnRef) {

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
    @Operation(
            summary = "Check transaction existence",
            description = """
                    Check if a transaction reference exists in the system.

                    **Use Cases:**
                    - Validate transaction reference before processing
                    - Check for duplicate transaction references
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Check completed",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApiResponse.class),
                            examples = {
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "Exists",
                                            value = """
                                                    {
                                                      "code": "999999",
                                                      "message": "Transaction existence checked",
                                                      "data": true
                                                    }
                                                    """
                                    ),
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "Does Not Exist",
                                            value = """
                                                    {
                                                      "code": "999999",
                                                      "message": "Transaction existence checked",
                                                      "data": false
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    })
    public ApiResponse<Boolean> checkTransactionExists(
            @Parameter(description = "Transaction reference to check", example = "TXN123456") @PathVariable String transactionRef) {

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
