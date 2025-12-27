package com.smartrent.service.payment.provider;

import com.smartrent.config.VNPayConfig;
import com.smartrent.dto.request.PaymentRequest;
import com.smartrent.dto.response.PaymentCallbackResponse;
import com.smartrent.dto.response.PaymentResponse;
import com.smartrent.enums.PaymentProvider;
import com.smartrent.enums.TransactionStatus;
import com.smartrent.infra.connector.VNPayConnector;
import com.smartrent.infra.connector.model.VNPayQueryRequest;
import com.smartrent.infra.connector.model.VNPayQueryResponse;
import com.smartrent.infra.exception.PaymentNotFoundException;
import com.smartrent.infra.exception.PaymentProviderException;
import com.smartrent.infra.exception.PaymentValidationException;
import com.smartrent.infra.repository.PaymentRepository;
import com.smartrent.infra.repository.entity.Transaction;
import com.smartrent.utility.PaymentUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * VNPay Payment Provider Implementation
 * Implements the PaymentProvider interface for VNPay payment gateway
 */
@Slf4j
@Service
public class VNPayPaymentProvider extends AbstractPaymentProvider {

    private final VNPayConfig vnpayConfig;
    private final VNPayConnector vnpayConnector;

    private static final DateTimeFormatter VN_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String VNP_VERSION = "2.1.0";
    private static final String VNP_COMMAND = "pay";
    private static final String VNP_QUERY_COMMAND = "querydr";
    private static final String VNP_CURRENCY_CODE = "VND";
    private static final String VNP_LOCALE_VN = "vn";

    public VNPayPaymentProvider(PaymentRepository paymentRepository, VNPayConfig vnpayConfig, VNPayConnector vnpayConnector) {
        super(paymentRepository);
        this.vnpayConfig = vnpayConfig;
        this.vnpayConnector = vnpayConnector;
    }

    @Override
    public PaymentProvider getProviderType() {
        return PaymentProvider.VNPAY;
    }

    @Override
    public PaymentResponse createPayment(PaymentRequest request, HttpServletRequest httpRequest) {
        log.info("Creating VNPay payment for amount: {} {}", request.getAmount(), request.getCurrency());

        // Validate request
        validatePaymentRequest(request);
        validateVNPayRequest(request);

        // Create payment record in database
        Transaction transaction = createPaymentRecord(request, httpRequest);

        // Build VNPay payment URL
        String paymentUrl = buildVNPayPaymentUrl(transaction, request, httpRequest);

        log.info("VNPay payment created successfully with txnRef: {}", transaction.getTransactionId());

        // Use current time for timestamps since @CreationTimestamp may not be populated yet
        // Use Vietnam timezone for consistency with VNPay
        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        Integer timeout = vnpayConfig.getTimeout() != null ? vnpayConfig.getTimeout() : 15;

        return PaymentResponse.builder()
                .provider(PaymentProvider.VNPAY)
                .transactionRef(transaction.getTransactionId())
                .paymentUrl(paymentUrl)
                .amount(transaction.getAmount())
                .currency(request.getCurrency())
                .orderInfo(transaction.getOrderInfo())
                .createdAt(now)
                .expiresAt(now.plusMinutes(timeout))
                .build();
    }

    @Override
    public PaymentCallbackResponse processIPN(Map<String, String> params, HttpServletRequest httpRequest) {
        log.info("Processing VNPay IPN callback");

        // Extract VNPay parameters
        String vnpTxnRef = params.get("vnp_TxnRef");
        String vnpTransactionNo = params.get("vnp_TransactionNo");
        String vnpResponseCode = params.get("vnp_ResponseCode");
        String vnpSecureHash = params.get("vnp_SecureHash");
        String vnpBankCode = params.get("vnp_BankCode");
        String vnpCardType = params.get("vnp_CardType");
        String vnpBankTranNo = params.get("vnp_BankTranNo");
        String vnpPayDate = params.get("vnp_PayDate");

        log.info("VNPay IPN - TxnRef: {}, TransactionNo: {}, ResponseCode: {}",
                vnpTxnRef, vnpTransactionNo, vnpResponseCode);

        // Validate signature using raw query string from request
        if (!validateSignatureFromRequest(httpRequest, vnpSecureHash)) {
            log.error("Invalid VNPay signature for transaction: {}", vnpTxnRef);
            return PaymentCallbackResponse.builder()
                    .provider(PaymentProvider.VNPAY)
                    .transactionRef(vnpTxnRef)
                    .success(false)
                    .signatureValid(false)
                    .message("Invalid VNPay signature")
                    .build();
        }

        // Find transaction
        Optional<Transaction> transactionOpt = findPaymentByTransactionRef(vnpTxnRef);
        if (transactionOpt.isEmpty()) {
            log.error("Transaction not found: {}", vnpTxnRef);
            return PaymentCallbackResponse.builder()
                    .provider(PaymentProvider.VNPAY)
                    .transactionRef(vnpTxnRef)
                    .success(false)
                    .signatureValid(true)
                    .message("Transaction not found")
                    .build();
        }

        // Determine transaction status based on response code
        TransactionStatus status = mapVNPayResponseCodeToStatus(vnpResponseCode);

        // Update transaction with VNPay data
        Transaction updatedTransaction = updatePaymentWithProviderData(
                vnpTxnRef,
                vnpTransactionNo,
                vnpCardType != null ? vnpCardType : "UNKNOWN",
                vnpBankCode != null ? vnpBankCode : "UNKNOWN",
                vnpBankTranNo != null ? vnpBankTranNo : "UNKNOWN",
                status,
                vnpResponseCode,
                getVNPayResponseMessage(vnpResponseCode)
        );

        log.info("VNPay IPN processed successfully - TxnRef: {}, Status: {}", vnpTxnRef, status);

        boolean isSuccess = TransactionStatus.COMPLETED.equals(status);
        String message = isSuccess ? "Payment processed successfully" : "Payment " + status.name().toLowerCase();

        return PaymentCallbackResponse.builder()
                .provider(PaymentProvider.VNPAY)
                .transactionRef(updatedTransaction.getTransactionId())
                .providerTransactionId(updatedTransaction.getProviderTransactionId())
                .status(updatedTransaction.getStatus())
                .amount(updatedTransaction.getAmount())
                .currency(VNP_CURRENCY_CODE)
                .orderInfo(updatedTransaction.getOrderInfo())
                .paymentMethod(vnpCardType)
                .bankCode(vnpBankCode)
                .bankTransactionId(vnpBankTranNo)
                .responseCode(vnpResponseCode)
                .responseMessage(getVNPayResponseMessage(vnpResponseCode))
                .paymentDate(parseVNPayDate(vnpPayDate))
                .success(isSuccess)
                .signatureValid(true)
                .message(message)
                .build();
    }

    @Override
    public PaymentCallbackResponse queryTransaction(String transactionRef) {
        log.info("Querying VNPay transaction: {}", transactionRef);

        // Find transaction in database
        Optional<Transaction> transactionOpt = findPaymentByTransactionRef(transactionRef);
        if (transactionOpt.isEmpty()) {
            throw new PaymentNotFoundException(transactionRef);
        }

        Transaction transaction = transactionOpt.get();

        try {
            // Build VNPay query request
            VNPayQueryRequest queryRequest = buildVNPayQueryRequest(transaction);

            // Call VNPay API to query transaction status
            VNPayQueryResponse queryResponse = vnpayConnector.queryTransaction(queryRequest);

            // Validate response signature
            if (!validateQueryResponseSignature(queryResponse)) {
                log.error("Invalid VNPay query response signature for transaction: {}", transactionRef);
                throw new PaymentValidationException("Invalid VNPay query response signature");
            }

            // Update transaction status based on VNPay response
            if ("00".equals(queryResponse.getVnp_ResponseCode())) {
                TransactionStatus status = mapVNPayTransactionStatus(queryResponse.getVnp_TransactionStatus());
                String responseCode = queryResponse.getVnp_ResponseCode();
                String responseMessage = queryResponse.getVnp_Message();

                // Update transaction if status changed
                if (!status.equals(transaction.getStatus())) {
                    transaction = updatePaymentWithProviderData(
                            transactionRef,
                            queryResponse.getVnp_TransactionNo(),
                            "UNKNOWN",
                            queryResponse.getVnp_BankCode() != null ? queryResponse.getVnp_BankCode() : "UNKNOWN",
                            queryResponse.getVnp_TransactionNo() != null ? queryResponse.getVnp_TransactionNo() : "UNKNOWN",
                            status,
                            responseCode,
                            responseMessage
                    );
                }

                log.info("VNPay transaction query successful - TxnRef: {}, Status: {}", transactionRef, status);

                boolean isSuccess = TransactionStatus.COMPLETED.equals(status);
                String message = "Transaction queried successfully";

                return PaymentCallbackResponse.builder()
                        .provider(PaymentProvider.VNPAY)
                        .transactionRef(transaction.getTransactionId())
                        .providerTransactionId(queryResponse.getVnp_TransactionNo())
                        .status(status)
                        .amount(transaction.getAmount())
                        .currency(VNP_CURRENCY_CODE)
                        .orderInfo(queryResponse.getVnp_OrderInfo())
                        .bankCode(queryResponse.getVnp_BankCode())
                        .responseCode(queryResponse.getVnp_ResponseCode())
                        .responseMessage(queryResponse.getVnp_Message())
                        .paymentDate(parseVNPayDate(queryResponse.getVnp_PayDate()))
                        .success(isSuccess)
                        .signatureValid(true)
                        .message(message)
                        .build();
            } else {
                // Query failed, return current database status
                log.warn("VNPay query failed - ResponseCode: {}, Message: {}",
                        queryResponse.getVnp_ResponseCode(), queryResponse.getVnp_Message());
            }
        } catch (Exception e) {
            log.error("Error querying VNPay transaction: {}", transactionRef, e);
            // Fall through to return database status
        }

        // Return current status from database
        log.info("VNPay transaction query completed (from DB) - TxnRef: {}, Status: {}",
                transactionRef, transaction.getStatus());

        boolean isSuccess = TransactionStatus.COMPLETED.equals(transaction.getStatus());
        String message = "Transaction status retrieved from database";

        return PaymentCallbackResponse.builder()
                .provider(PaymentProvider.VNPAY)
                .transactionRef(transaction.getTransactionId())
                .providerTransactionId(transaction.getProviderTransactionId())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .currency(VNP_CURRENCY_CODE)
                .orderInfo(transaction.getOrderInfo())
                .success(isSuccess)
                .signatureValid(true)
                .message(message)
                .build();
    }

    /**
     * Validate signature using raw query string from HttpServletRequest
     * VNPay calculates signature using the raw URL-encoded query string
     * We must use the exact same raw values (not decode and re-encode)
     */
    private boolean validateSignatureFromRequest(HttpServletRequest request, String signature) {
        try {
            String queryString = request.getQueryString();
            if (queryString == null || queryString.isEmpty()) {
                log.error("Query string is empty");
                return false;
            }

            log.info("VNPay raw query string: {}", queryString);

            // Log hash secret info for debugging (masked)
            String hashSecret = vnpayConfig.getHashSecret();
            log.info("VNPay hash secret length: {}, first 4 chars: {}",
                    hashSecret != null ? hashSecret.length() : 0,
                    hashSecret != null && hashSecret.length() >= 4 ? hashSecret.substring(0, 4) + "***" : "N/A");

            // Parse query string - keep raw URL-encoded values (DO NOT decode)
            // VNPay signature is calculated on the raw URL-encoded string
            Map<String, String> params = new TreeMap<>();
            String[] pairs = queryString.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                if (idx > 0) {
                    String key = pair.substring(0, idx);
                    // Keep the raw URL-encoded value as-is
                    String rawValue = idx < pair.length() - 1 ? pair.substring(idx + 1) : "";
                    // Only include VNPay parameters, exclude signature
                    if (key.startsWith("vnp_") && !key.equals("vnp_SecureHash") && !key.equals("vnp_SecureHashType")) {
                        params.put(key, rawValue);
                    }
                }
            }

            // Build hash data using raw URL-encoded values (no re-encoding needed)
            StringBuilder hashData = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    if (!first) {
                        hashData.append('&');
                    }
                    hashData.append(entry.getKey());
                    hashData.append('=');
                    hashData.append(entry.getValue());
                    first = false;
                }
            }

            log.info("VNPay signature validation - Hash data (raw): {}", hashData);

            // Generate signature
            String generatedSignature = PaymentUtil.hmacSHA512(hashSecret, hashData.toString());

            log.info("VNPay signature validation - Generated: {}, Received: {}", generatedSignature, signature);

            boolean isValid = generatedSignature.equalsIgnoreCase(signature);

            if (!isValid) {
                log.warn("VNPay signature validation failed. Generated: {}, Received: {}",
                        generatedSignature, signature);
            }

            return isValid;
        } catch (Exception e) {
            log.error("Error validating VNPay signature from request", e);
            return false;
        }
    }

    @Override
    public boolean validateSignature(Map<String, String> params, String signature) {
        try {
            // Remove secure hash and non-VNPay params from validation
            Map<String, String> validationParams = new TreeMap<>(params);
            validationParams.remove("vnp_SecureHash");
            validationParams.remove("vnp_SecureHashType");

            // Remove non-VNPay parameters (they should not be included in signature)
            validationParams.entrySet().removeIf(entry -> !entry.getKey().startsWith("vnp_"));

            // Build hash data string (without URL encoding - for decoded params)
            String hashData = buildHashDataForCallback(validationParams);

            log.info("VNPay signature validation - Hash data: {}", hashData);

            // Generate signature
            String generatedSignature = PaymentUtil.hmacSHA512(vnpayConfig.getHashSecret(), hashData);

            log.info("VNPay signature validation - Generated: {}, Received: {}", generatedSignature, signature);

            boolean isValid = generatedSignature.equalsIgnoreCase(signature);

            if (!isValid) {
                log.warn("VNPay signature validation failed. Generated: {}, Received: {}",
                        generatedSignature, signature);
            }

            return isValid;
        } catch (Exception e) {
            log.error("Error validating VNPay signature", e);
            return false;
        }
    }

    @Override
    public boolean cancelPayment(String transactionRef, String reason) {
        log.info("Cancelling VNPay payment: {}", transactionRef);

        try {
            updatePaymentStatus(
                    transactionRef,
                    TransactionStatus.CANCELLED,
                    "CANCELLED",
                    reason != null ? reason : "Payment cancelled by user"
            );

            log.info("VNPay payment cancelled successfully: {}", transactionRef);
            return true;
        } catch (Exception e) {
            log.error("Error cancelling VNPay payment: {}", transactionRef, e);
            return false;
        }
    }

    @Override
    public PaymentCallbackResponse refundPayment(String transactionRef, String amount, String reason) {
        // VNPay refund requires special merchant permissions and API integration
        // This is typically done through VNPay's merchant portal or dedicated refund API
        throw PaymentProviderException.operationNotSupported("Refund", "VNPay");
    }

    @Override
    public boolean supportsFeature(PaymentFeature feature) {
        return switch (feature) {
            case QR_CODE, BANK_TRANSFER, CREDIT_CARD, DEBIT_CARD, E_WALLET -> true;
            case REFUND, PARTIAL_REFUND, RECURRING_PAYMENT, INSTALLMENT -> false;
            default -> false;
        };
    }

    @Override
    public Map<String, Object> getConfigurationSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("provider", "VNPay");
        schema.put("tmnCode", vnpayConfig.getTmnCode());
        String paymentUrl = vnpayConfig.getPaymentUrl() != null ? vnpayConfig.getPaymentUrl() : vnpayConfig.getUrl();
        schema.put("paymentUrl", paymentUrl);
        schema.put("returnUrl", vnpayConfig.getReturnUrl());
        schema.put("version", vnpayConfig.getVersion());
        schema.put("currency", VNP_CURRENCY_CODE);
        schema.put("locale", vnpayConfig.getLocale());
        schema.put("timeout", vnpayConfig.getTimeout());
        return schema;
    }

    @Override
    public boolean validateConfiguration() {
        if (vnpayConfig.getTmnCode() == null || vnpayConfig.getTmnCode().isEmpty()) {
            log.error("VNPay TMN Code is not configured");
            return false;
        }
        if (vnpayConfig.getHashSecret() == null || vnpayConfig.getHashSecret().isEmpty()) {
            log.error("VNPay Hash Secret is not configured");
            return false;
        }
        String paymentUrl = vnpayConfig.getPaymentUrl() != null ? vnpayConfig.getPaymentUrl() : vnpayConfig.getUrl();
        if (paymentUrl == null || paymentUrl.isEmpty()) {
            log.error("VNPay Payment URL is not configured");
            return false;
        }
        return true;
    }

    // Private helper methods

    private void validateVNPayRequest(PaymentRequest request) {
        // VNPay specific validations
        if (!"VND".equals(request.getCurrency())) {
            throw PaymentValidationException.invalidCurrency(request.getCurrency());
        }

        // VNPay amount must be in VND (no decimal points)
        if (request.getAmount().scale() > 0) {
            throw new PaymentValidationException("VNPay amount must be a whole number (VND)");
        }

        // VNPay minimum amount is 1,000 VND
        if (request.getAmount().compareTo(new BigDecimal("1000")) < 0) {
            throw new PaymentValidationException("VNPay minimum amount is 1,000 VND");
        }
    }

    private String buildVNPayPaymentUrl(Transaction transaction, PaymentRequest request, HttpServletRequest httpRequest) {
        try {
            Map<String, String> vnpParams = new TreeMap<>();

            // Required parameters
            vnpParams.put("vnp_Version", vnpayConfig.getVersion() != null ? vnpayConfig.getVersion() : VNP_VERSION);
            vnpParams.put("vnp_Command", vnpayConfig.getCommand() != null ? vnpayConfig.getCommand() : VNP_COMMAND);
            vnpParams.put("vnp_TmnCode", vnpayConfig.getTmnCode());
            vnpParams.put("vnp_Amount", String.valueOf(request.getAmount().multiply(new BigDecimal("100")).longValue()));
            vnpParams.put("vnp_CurrCode", VNP_CURRENCY_CODE);
            vnpParams.put("vnp_TxnRef", transaction.getTransactionId());
            vnpParams.put("vnp_OrderInfo", request.getOrderInfo());
            vnpParams.put("vnp_OrderType", vnpayConfig.getOrderType() != null ? vnpayConfig.getOrderType() : "other");
            vnpParams.put("vnp_Locale", vnpayConfig.getLocale() != null ? vnpayConfig.getLocale() : VNP_LOCALE_VN);

            // Return URL (for user redirect after payment)
            // After payment, VNPAY redirects user to this URL with payment result parameters
            // Frontend should then call GET /v1/payments/callback/VNPAY with those parameters
            String returnUrl = request.getReturnUrl() != null ? request.getReturnUrl() : vnpayConfig.getReturnUrl();
            vnpParams.put("vnp_ReturnUrl", returnUrl);

            // IP Address
            String ipAddress = getClientIpAddress(httpRequest);
            vnpParams.put("vnp_IpAddr", ipAddress);

            // Create date - MUST use Vietnam timezone for VNPay
            LocalDateTime vietnamNow = LocalDateTime.now(VIETNAM_ZONE);
            String createDate = vietnamNow.format(VN_DATE_FORMATTER);
            vnpParams.put("vnp_CreateDate", createDate);

            // Expire date (default 15 minutes)
            Integer timeout = vnpayConfig.getTimeout() != null ? vnpayConfig.getTimeout() : 15;
            String expireDate = vietnamNow.plusMinutes(timeout).format(VN_DATE_FORMATTER);
            vnpParams.put("vnp_ExpireDate", expireDate);

            // Build hash data (BEFORE adding secure hash to params)
            String hashData = buildHashData(vnpParams);

            // Generate secure hash
            String secureHash = PaymentUtil.hmacSHA512(vnpayConfig.getHashSecret(), hashData);

            // Build query string (with URL encoding)
            String queryUrl = buildQueryString(vnpParams);

            // Build final payment URL (use paymentUrl property, fallback to url)
            String paymentUrl = vnpayConfig.getPaymentUrl() != null ? vnpayConfig.getPaymentUrl() : vnpayConfig.getUrl();
            if (!paymentUrl.endsWith("?")) {
                paymentUrl += "?";
            }
            // Append query string and secure hash (secure hash is NOT URL encoded)
            paymentUrl += queryUrl + "&vnp_SecureHash=" + secureHash;

            log.debug("VNPay payment URL generated for txnRef: {}", transaction.getTransactionId());
            return paymentUrl;

        } catch (Exception e) {
            log.error("Error building VNPay payment URL", e);
            throw new PaymentProviderException("Failed to generate VNPay payment URL", e);
        }
    }

    /**
     * Build hash data for payment creation (WITH URL encoding)
     * Used when creating payment URL to send to VNPay
     */
    private String buildHashData(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    try {
                        return URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.toString()) + "="
                            + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.toString());
                    } catch (UnsupportedEncodingException e) {
                        log.error("Error encoding hash parameter: {}", entry.getKey(), e);
                        return entry.getKey() + "=" + entry.getValue();
                    }
                })
                .collect(Collectors.joining("&"));
    }

    /**
     * Build hash data for callback validation (WITHOUT URL encoding)
     * Used when validating signature from VNPay callback
     * The parameters are already decoded by Spring, so we use them as-is
     */
    private String buildHashDataForCallback(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }

    private String buildQueryString(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    try {
                        return entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.toString());
                    } catch (UnsupportedEncodingException e) {
                        log.error("Error encoding URL parameter: {}", entry.getKey(), e);
                        return entry.getKey() + "=" + entry.getValue();
                    }
                })
                .collect(Collectors.joining("&"));
    }

    private TransactionStatus mapVNPayResponseCodeToStatus(String responseCode) {
        return switch (responseCode) {
            case "00" -> TransactionStatus.COMPLETED;
            case "07" -> TransactionStatus.PENDING; // Trừ tiền thành công, đang chờ xác nhận
            case "09" -> TransactionStatus.PENDING; // Giao dịch chưa hoàn tất
            case "24" -> TransactionStatus.CANCELLED; // Khách hàng hủy giao dịch
            case "11", "12", "13", "51", "65", "75", "79" -> TransactionStatus.FAILED; // Various failure codes
            default -> TransactionStatus.FAILED;
        };
    }

    private String getVNPayResponseMessage(String responseCode) {
        return switch (responseCode) {
            case "00" -> "Giao dịch thành công";
            case "07" -> "Trừ tiền thành công. Giao dịch đang được xác nhận";
            case "09" -> "Giao dịch chưa hoàn tất";
            case "10" -> "Giao dịch không thành công do: Khách hàng nhập sai thông tin thẻ/tài khoản";
            case "11" -> "Giao dịch không thành công do: Đã hết hạn chờ thanh toán";
            case "12" -> "Giao dịch không thành công do: Thẻ/Tài khoản bị khóa";
            case "13" -> "Giao dịch không thành công do: Quý khách nhập sai mật khẩu xác thực giao dịch (OTP)";
            case "24" -> "Giao dịch không thành công do: Khách hàng hủy giao dịch";
            case "51" -> "Giao dịch không thành công do: Tài khoản không đủ số dư";
            case "65" -> "Giao dịch không thành công do: Tài khoản đã vượt quá hạn mức giao dịch";
            case "75" -> "Ngân hàng thanh toán đang bảo trì";
            case "79" -> "Giao dịch không thành công do: KH nhập sai mật khẩu thanh toán quá số lần quy định";
            case "99" -> "Các lỗi khác";
            default -> "Lỗi không xác định";
        };
    }

    private LocalDateTime parseVNPayDate(String vnpayDate) {
        if (vnpayDate == null || vnpayDate.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(vnpayDate, VN_DATE_FORMATTER);
        } catch (Exception e) {
            log.error("Error parsing VNPay date: {}", vnpayDate, e);
            return null;
        }
    }

    private VNPayQueryRequest buildVNPayQueryRequest(Transaction transaction) {
        String requestId = PaymentUtil.generateRandomString(8);
        // MUST use Vietnam timezone for VNPay
        String createDate = LocalDateTime.now(VIETNAM_ZONE).format(VN_DATE_FORMATTER);
        String ipAddress = "127.0.0.1"; // Default IP for query requests

        // Get transaction date from database
        String transactionDate = transaction.getCreatedAt().format(VN_DATE_FORMATTER);

        // Build query parameters (sorted for hashing)
        Map<String, String> queryParams = new TreeMap<>();
        queryParams.put("vnp_RequestId", requestId);
        queryParams.put("vnp_Version", vnpayConfig.getVersion() != null ? vnpayConfig.getVersion() : VNP_VERSION);
        queryParams.put("vnp_Command", VNP_QUERY_COMMAND);
        queryParams.put("vnp_TmnCode", vnpayConfig.getTmnCode());
        queryParams.put("vnp_TxnRef", transaction.getTransactionId());
        queryParams.put("vnp_OrderInfo", transaction.getOrderInfo() != null ? transaction.getOrderInfo() : "");
        queryParams.put("vnp_TransactionDate", transactionDate);
        queryParams.put("vnp_CreateDate", createDate);
        queryParams.put("vnp_IpAddr", ipAddress);

        // Build hash data and generate signature
        String hashData = buildHashData(queryParams);
        String secureHash = PaymentUtil.hmacSHA512(vnpayConfig.getHashSecret(), hashData);

        return VNPayQueryRequest.builder()
                .vnp_RequestId(requestId)
                .vnp_Version(vnpayConfig.getVersion() != null ? vnpayConfig.getVersion() : VNP_VERSION)
                .vnp_Command(VNP_QUERY_COMMAND)
                .vnp_TmnCode(vnpayConfig.getTmnCode())
                .vnp_TxnRef(transaction.getTransactionId())
                .vnp_OrderInfo(transaction.getOrderInfo() != null ? transaction.getOrderInfo() : "")
                .vnp_TransactionDate(transactionDate)
                .vnp_CreateDate(createDate)
                .vnp_IpAddr(ipAddress)
                .vnp_SecureHash(secureHash)
                .build();
    }

    private boolean validateQueryResponseSignature(VNPayQueryResponse response) {
        try {
            String receivedHash = response.getVnp_SecureHash();
            if (receivedHash == null) {
                return false;
            }

            // Build hash data from response (exclude vnp_SecureHash)
            Map<String, String> responseParams = new TreeMap<>();
            if (response.getVnp_ResponseId() != null) responseParams.put("vnp_ResponseId", response.getVnp_ResponseId());
            if (response.getVnp_Command() != null) responseParams.put("vnp_Command", response.getVnp_Command());
            if (response.getVnp_ResponseCode() != null) responseParams.put("vnp_ResponseCode", response.getVnp_ResponseCode());
            if (response.getVnp_Message() != null) responseParams.put("vnp_Message", response.getVnp_Message());
            if (response.getVnp_TmnCode() != null) responseParams.put("vnp_TmnCode", response.getVnp_TmnCode());
            if (response.getVnp_TxnRef() != null) responseParams.put("vnp_TxnRef", response.getVnp_TxnRef());
            if (response.getVnp_Amount() != null) responseParams.put("vnp_Amount", response.getVnp_Amount());
            if (response.getVnp_OrderInfo() != null) responseParams.put("vnp_OrderInfo", response.getVnp_OrderInfo());
            if (response.getVnp_BankCode() != null) responseParams.put("vnp_BankCode", response.getVnp_BankCode());
            if (response.getVnp_PayDate() != null) responseParams.put("vnp_PayDate", response.getVnp_PayDate());
            if (response.getVnp_TransactionNo() != null) responseParams.put("vnp_TransactionNo", response.getVnp_TransactionNo());
            if (response.getVnp_TransactionType() != null) responseParams.put("vnp_TransactionType", response.getVnp_TransactionType());
            if (response.getVnp_TransactionStatus() != null) responseParams.put("vnp_TransactionStatus", response.getVnp_TransactionStatus());

            // Use callback method for query response (no URL encoding needed)
            String hashData = buildHashDataForCallback(responseParams);
            String computedHash = PaymentUtil.hmacSHA512(vnpayConfig.getHashSecret(), hashData);

            return computedHash.equalsIgnoreCase(receivedHash);
        } catch (Exception e) {
            log.error("Error validating VNPay query response signature", e);
            return false;
        }
    }

    private TransactionStatus mapVNPayTransactionStatus(String transactionStatus) {
        if (transactionStatus == null) {
            return TransactionStatus.PENDING;
        }

        return switch (transactionStatus) {
            case "00" -> TransactionStatus.COMPLETED; // Transaction successful
            case "01" -> TransactionStatus.PENDING;   // Transaction not completed
            case "02" -> TransactionStatus.FAILED;    // Transaction failed
            case "04" -> TransactionStatus.CANCELLED; // Transaction reversed (cancelled)
            case "05" -> TransactionStatus.PENDING;   // Transaction is being processed by VNPAY (for refund)
            case "06" -> TransactionStatus.PENDING;   // Customer requested refund
            case "07" -> TransactionStatus.PENDING;   // Transaction suspicious (waiting for verification)
            case "09" -> TransactionStatus.REFUNDED;  // Refund request rejected
            default -> TransactionStatus.PENDING;
        };
    }
}
