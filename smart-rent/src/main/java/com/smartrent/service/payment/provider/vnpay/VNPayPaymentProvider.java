package com.smartrent.service.payment.provider.vnpay;

import com.smartrent.dto.request.PaymentRequest;
import com.smartrent.dto.response.PaymentCallbackResponse;
import com.smartrent.dto.response.PaymentResponse;
import com.smartrent.enums.TransactionStatus;
import com.smartrent.infra.connector.VNPayConnector;
import com.smartrent.infra.connector.model.VNPayQueryRequest;
import com.smartrent.infra.connector.model.VNPayQueryResponse;
import com.smartrent.infra.repository.PaymentRepository;
import com.smartrent.infra.repository.entity.Transaction;
import com.smartrent.service.payment.provider.AbstractPaymentProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class VNPayPaymentProvider extends AbstractPaymentProvider {

    private final VNPayProperties vnPayProperties;
    private final VNPayConnector vnPayConnector;

    public VNPayPaymentProvider(PaymentRepository paymentRepository,
                               VNPayProperties vnPayProperties,
                               VNPayConnector vnPayConnector) {
        super(paymentRepository);
        this.vnPayProperties = vnPayProperties;
        this.vnPayConnector = vnPayConnector;
    }

    @Override
    public com.smartrent.enums.PaymentProvider getProviderType() {
        return com.smartrent.enums.PaymentProvider.VNPAY;
    }

    @Override
    public PaymentResponse createPayment(PaymentRequest request, HttpServletRequest httpRequest) {
        log.info("Creating VNPay payment for amount: {}", request.getAmount());

        try {
            // Create payment record
            Transaction transaction = createPaymentRecord(request, httpRequest);
            String ipAddress = getClientIpAddress(httpRequest);

            // Build VNPay parameters
            Map<String, String> vnpParams = buildVNPayParams(transaction, request, ipAddress);

            // Generate secure hash (do NOT add it to vnpParams)
            String secureHash = VNPayUtil.generateSecureHash(vnpParams, vnPayProperties.getHashSecret());

            // Build payment URL with query string + secure hash at the end
            String queryString = VNPayUtil.buildQueryString(vnpParams);
            String paymentUrl = vnPayProperties.getPaymentUrl() + "?" + queryString + "&vnp_SecureHash=" + secureHash;

            log.info("VNPay payment created successfully. Transaction ref: {}, IP: {}", transaction.getTransactionId(), ipAddress);
            log.debug("VNPay query string for hash: {}", queryString);

            // Use current time for createdAt and expiresAt since @CreationTimestamp may not be populated yet
            java.time.LocalDateTime now = java.time.LocalDateTime.now();

            return PaymentResponse.builder()
                    .provider(getProviderType())
                    .transactionRef(transaction.getTransactionId())
                    .paymentUrl(paymentUrl)
                    .amount(transaction.getAmount())
                    .currency("VND") // Default currency
                    .orderInfo(request.getOrderInfo())
                    .createdAt(now)
                    .expiresAt(now.plusMinutes(15)) // VNPay default expiry
                    .build();

        } catch (Exception e) {
            log.error("Error creating VNPay payment", e);
            throw new RuntimeException("Failed to create VNPay payment", e);
        }
    }

    @Override
    public PaymentCallbackResponse processIPN(Map<String, String> params, HttpServletRequest httpRequest) {
        String txnRef = params.get("vnp_TxnRef");
        log.info("Processing VNPay IPN for transaction: {}", txnRef);

        String secureHash = params.get("vnp_SecureHash");

        // Validate signature
        boolean signatureValid = validateSignature(params, secureHash);
        if (!signatureValid) {
            log.warn("Invalid signature for IPN transaction: {}", txnRef);
            return PaymentCallbackResponse.builder()
                    .provider(getProviderType())
                    .success(false)
                    .signatureValid(false)
                    .message("Invalid signature")
                    .build();
        }

        // Find and update transaction
        Optional<Transaction> transactionOpt = findPaymentByTransactionRef(txnRef);
        if (transactionOpt.isEmpty()) {
            log.warn("Transaction not found for IPN transaction: {}", txnRef);
            return PaymentCallbackResponse.builder()
                    .provider(getProviderType())
                    .success(false)
                    .signatureValid(true)
                    .message("Transaction not found")
                    .build();
        }

        Transaction transaction = transactionOpt.get();
        String responseCode = params.get("vnp_ResponseCode");
        VNPayTransactionStatus status = VNPayTransactionStatus.fromCode(responseCode);
        TransactionStatus genericStatus = VNPayStatusConverter.toGenericStatus(status);

        log.info("VNPay IPN - txnRef: {}, responseCode: {}, vnpayStatus: {}, genericStatus: {}",
                 txnRef, responseCode, status, genericStatus);

        transaction = updatePaymentWithProviderData(
                txnRef,
                params.get("vnp_TransactionNo"),
                params.get("vnp_CardType"),
                params.get("vnp_BankCode"),
                params.get("vnp_BankTranNo"),
                genericStatus,
                responseCode,
                getResponseMessage(responseCode)
        );

        boolean success = VNPayUtil.isSuccessResponseCode(responseCode);
        log.info("VNPay IPN processing completed - txnRef: {}, success: {}, finalStatus: {}",
                 txnRef, success, transaction.getStatus());

        return buildCallbackResponse(transaction, true, success ? "IPN processed successfully" : "Payment failed");
    }

    @Override
    public PaymentCallbackResponse queryTransaction(String transactionRef) {
        log.info("Querying VNPay transaction: {}", transactionRef);

        try {
            Optional<Transaction> transactionOpt = findPaymentByTransactionRef(transactionRef);
            if (transactionOpt.isEmpty()) {
                return PaymentCallbackResponse.builder()
                        .provider(getProviderType())
                        .success(false)
                        .message("Transaction not found")
                        .build();
            }

            Transaction transaction = transactionOpt.get();

            // Build query request
            VNPayQueryRequest queryRequest = VNPayQueryRequest.builder()
                    .vnp_RequestId(VNPayUtil.generateRequestId())
                    .vnp_Version(vnPayProperties.getVersion())
                    .vnp_Command("querydr")
                    .vnp_TmnCode(vnPayProperties.getTmnCode())
                    .vnp_TxnRef(transactionRef)
                    .vnp_OrderInfo(transaction.getOrderInfo())
                    .vnp_TransactionDate(VNPayUtil.formatDateTime(transaction.getCreatedAt()))
                    .vnp_CreateDate(VNPayUtil.formatCurrentDateTime())
                    .vnp_IpAddr(transaction.getIpAddress())
                    .build();

            // Generate secure hash for query
            Map<String, String> queryParams = buildQueryParams(queryRequest);
            String secureHash = VNPayUtil.generateSecureHash(queryParams, vnPayProperties.getHashSecret());
            queryRequest.setVnp_SecureHash(secureHash);

            // Query VNPay
            VNPayQueryResponse response = vnPayConnector.queryTransaction(queryRequest);

            // Update transaction if status changed
            if (response.getVnp_ResponseCode().equals("00")) {
                VNPayTransactionStatus newStatus = VNPayTransactionStatus.fromCode(response.getVnp_TransactionStatus());
                if (!transaction.getStatus().equals(VNPayStatusConverter.toGenericStatus(newStatus))) {
                    transaction = updatePaymentStatus(transactionRef, VNPayStatusConverter.toGenericStatus(newStatus),
                            response.getVnp_ResponseCode(), response.getVnp_Message());
                }
            }

            return buildCallbackResponse(transaction, true, "Query completed");

        } catch (Exception e) {
            log.error("Error querying VNPay transaction: {}", transactionRef, e);
            throw new RuntimeException("Failed to query VNPay transaction", e);
        }
    }

    @Override
    public boolean validateSignature(Map<String, String> params, String signature) {
        return VNPayUtil.validateSecureHash(params, vnPayProperties.getHashSecret(), signature);
    }

    @Override
    public boolean cancelPayment(String transactionRef, String reason) {
        log.info("Cancelling VNPay payment: {}", transactionRef);

        try {
            Optional<Transaction> transactionOpt = findPaymentByTransactionRef(transactionRef);
            if (transactionOpt.isEmpty()) {
                return false;
            }

            Transaction transaction = transactionOpt.get();
            if (!transaction.isPending()) {
                log.warn("Cannot cancel non-pending VNPay payment: {}", transactionRef);
                return false;
            }

            updatePaymentStatus(transactionRef,
                    TransactionStatus.CANCELLED, "24", reason);
            log.info("VNPay payment cancelled successfully: {}", transactionRef);
            return true;

        } catch (Exception e) {
            log.error("Error cancelling VNPay payment: {}", transactionRef, e);
            return false;
        }
    }

    @Override
    public boolean supportsFeature(PaymentFeature feature) {
        Set<PaymentFeature> supportedFeatures = Set.of(
                PaymentFeature.QR_CODE,
                PaymentFeature.MOBILE_PAYMENT,
                PaymentFeature.BANK_TRANSFER,
                PaymentFeature.CREDIT_CARD,
                PaymentFeature.DEBIT_CARD,
                PaymentFeature.E_WALLET
        );
        return supportedFeatures.contains(feature);
    }

    @Override
    public Map<String, Object> getConfigurationSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("tmnCode", Map.of("type", "string", "required", true, "description", "VNPay Terminal Code"));
        schema.put("hashSecret", Map.of("type", "string", "required", true, "description", "VNPay Hash Secret"));
        schema.put("paymentUrl", Map.of("type", "string", "required", true, "description", "VNPay Payment URL"));
        schema.put("returnUrl", Map.of("type", "string", "required", true, "description", "Return URL"));
        schema.put("ipnUrl", Map.of("type", "string", "required", true, "description", "IPN URL"));
        return schema;
    }

    @Override
    public boolean validateConfiguration() {
        return vnPayProperties.getTmnCode() != null && !vnPayProperties.getTmnCode().isEmpty() &&
               vnPayProperties.getHashSecret() != null && !vnPayProperties.getHashSecret().isEmpty() &&
               vnPayProperties.getPaymentUrl() != null && !vnPayProperties.getPaymentUrl().isEmpty();
    }

    // Private helper methods

    private Map<String, String> buildVNPayParams(Transaction transaction, PaymentRequest request, String ipAddress) {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", vnPayProperties.getVersion());
        params.put("vnp_Command", vnPayProperties.getCommand());
        params.put("vnp_TmnCode", vnPayProperties.getTmnCode());
        params.put("vnp_Amount", VNPayUtil.formatAmount(transaction.getAmount().longValue()));
        params.put("vnp_CurrCode", vnPayProperties.getCurrencyCode());
        params.put("vnp_TxnRef", transaction.getTransactionId());
        params.put("vnp_OrderInfo", request.getOrderInfo());
        params.put("vnp_OrderType", vnPayProperties.getOrderType());
        params.put("vnp_Locale", getLanguageFromMetadata(request));
        params.put("vnp_ReturnUrl", request.getReturnUrl() != null ? request.getReturnUrl() : vnPayProperties.getReturnUrl());
        params.put("vnp_IpnUrl", vnPayProperties.getIpnUrl());
        params.put("vnp_IpAddr", ipAddress);
        params.put("vnp_CreateDate", VNPayUtil.formatCurrentDateTime());

        // Add bank code if specified in provider params
        if (request.getProviderParams() != null && request.getProviderParams().containsKey("bankCode")) {
            params.put("vnp_BankCode", request.getProviderParams().get("bankCode").toString());
        }

        return params;
    }

    private Map<String, String> buildQueryParams(VNPayQueryRequest queryRequest) {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_RequestId", queryRequest.getVnp_RequestId());
        params.put("vnp_Version", queryRequest.getVnp_Version());
        params.put("vnp_Command", queryRequest.getVnp_Command());
        params.put("vnp_TmnCode", queryRequest.getVnp_TmnCode());
        params.put("vnp_TxnRef", queryRequest.getVnp_TxnRef());
        params.put("vnp_OrderInfo", queryRequest.getVnp_OrderInfo());
        params.put("vnp_TransactionDate", queryRequest.getVnp_TransactionDate());
        params.put("vnp_CreateDate", queryRequest.getVnp_CreateDate());
        params.put("vnp_IpAddr", queryRequest.getVnp_IpAddr());
        return params;
    }

    private PaymentCallbackResponse buildCallbackResponse(Transaction transaction, boolean signatureValid, String message) {
        if (transaction == null) {
            return PaymentCallbackResponse.builder()
                    .provider(getProviderType())
                    .success(false)
                    .signatureValid(signatureValid)
                    .message(message)
                    .build();
        }

        return PaymentCallbackResponse.builder()
                .provider(getProviderType())
                .transactionRef(transaction.getTransactionId())
                .providerTransactionId(transaction.getProviderTransactionId())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .currency("VND") // Default currency for VNPay
                .success(transaction.isCompleted())
                .signatureValid(signatureValid)
                .message(message)
                .build();
    }

    private String getLanguageFromMetadata(PaymentRequest request) {
        if (request.getProviderParams() != null && request.getProviderParams().containsKey("language")) {
            return request.getProviderParams().get("language").toString();
        }
        return vnPayProperties.getLocale();
    }

    private String getResponseMessage(String responseCode) {
        // Map VNPay response codes to user-friendly messages
        return switch (responseCode) {
            case "00" -> "Transaction successful";
            case "07" -> "Deduct money successfully. Transaction is suspected (related to fraud, unusual transactions)";
            case "09" -> "Transaction failed: Customer's card/account has not registered for InternetBanking service at bank";
            case "10" -> "Transaction failed: Customer incorrectly authenticated transaction information more than 3 times";
            case "11" -> "Transaction failed: Payment deadline has expired";
            case "12" -> "Transaction failed: Customer's card/account is locked";
            case "13" -> "Transaction failed: Incorrect transaction authentication password";
            case "24" -> "Transaction canceled";
            case "51" -> "Transaction failed: Customer's account has insufficient balance";
            case "65" -> "Transaction failed: Customer has exceeded the daily transaction limit";
            case "75" -> "Payment Bank is under maintenance";
            case "79" -> "Transaction failed: Customer entered payment password incorrectly more than specified times";
            default -> "Unknown transaction status";
        };
    }

}
