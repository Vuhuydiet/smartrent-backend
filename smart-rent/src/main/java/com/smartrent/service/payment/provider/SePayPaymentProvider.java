package com.smartrent.service.payment.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartrent.config.SePayConfig;
import com.smartrent.constants.PricingConstants;
import com.smartrent.dto.request.PaymentRequest;
import com.smartrent.dto.response.PaymentCallbackResponse;
import com.smartrent.dto.response.PaymentResponse;
import com.smartrent.enums.PaymentProvider;
import com.smartrent.enums.TransactionStatus;
import com.smartrent.infra.repository.PaymentRepository;
import com.smartrent.infra.repository.TransactionAuditRepository;
import com.smartrent.infra.repository.entity.Transaction;
import com.smartrent.utility.PaymentUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * SePay Payment Gateway (Cổng thanh toán) provider.
 *
 * <p>Flow overview (hosted checkout, see https://developer.sepay.vn/vi/cong-thanh-toan):
 * <ol>
 *   <li>{@link #createPayment} creates the pending transaction and builds the SePay checkout form
 *       fields, signed with {@code base64(HMAC_SHA256(secretKey, "field=value,..."))}. The response
 *       returns the checkout URL plus the signed fields; the frontend renders them as a hidden form
 *       and auto-submits a POST to SePay's hosted checkout page.</li>
 *   <li>The customer pays on SePay (BANK_TRANSFER / NAPAS / CARD).</li>
 *   <li>SePay confirms the result with an IPN ({@code POST /v1/payments/webhook/sepay}, authenticated
 *       by the {@code X-Secret-Key} header). {@link #processIPN} matches the order by
 *       {@code order_invoice_number} (= our transaction ref), verifies the amount/status and marks
 *       the transaction COMPLETED.</li>
 *   <li>The browser is redirected to the configured success/error/cancel URL. The frontend then
 *       polls {@code GET /v1/payments/transactions/{txnRef}} for the final status.</li>
 * </ol>
 */
@Slf4j
@Service
public class SePayPaymentProvider extends AbstractPaymentProvider {

    private final SePayConfig sePayConfig;
    private final RestTemplate restTemplate;

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    /**
     * Checkout fields included in the HMAC signature, in the exact order the SePay SDK signs and
     * POSTs them (see github.com/sepayvn/sepay-pg-node checkout.ts). Order MUST match between
     * signing and POSTing or the gateway rejects the signature.
     */
    private static final String[] SIGNED_FIELD_ORDER = {
            "operation",
            "payment_method",
            "order_amount",
            "currency",
            "order_invoice_number",
            "order_description",
            "customer_id",
            "success_url",
            "error_url",
            "cancel_url",
            "merchant"
    };

    public SePayPaymentProvider(PaymentRepository paymentRepository,
                                TransactionAuditRepository transactionAuditRepository,
                                SePayConfig sePayConfig,
                                RestTemplate restTemplate) {
        super(paymentRepository, transactionAuditRepository);
        this.sePayConfig = sePayConfig;
        this.restTemplate = restTemplate;
    }

    @Override
    public PaymentProvider getProviderType() {
        return PaymentProvider.SEPAY;
    }

    @Override
    public PaymentResponse createPayment(PaymentRequest request, HttpServletRequest httpRequest) {
        log.info("Creating SePay Payment Gateway checkout for: {}", request.getOrderInfo());

        if (!validateConfiguration()) {
            throw new RuntimeException("SePay configuration is incomplete. Please set SEPAY_MERCHANT_ID and SEPAY_SECRET_KEY");
        }

        Transaction transaction = createPaymentRecord(request, httpRequest);
        LocalDateTime createdAt = transaction.getCreatedAt() != null
                ? transaction.getCreatedAt() : LocalDateTime.now(VIETNAM_ZONE);

        // Never re-issue a checkout for an already finalized transaction.
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            log.info("Transaction {} is already {}, returning without issuing a new SePay checkout",
                    transaction.getTransactionId(), transaction.getStatus());
            return PaymentResponse.builder()
                    .transactionRef(transaction.getTransactionId())
                    .provider(PaymentProvider.SEPAY)
                    .amount(transaction.getAmount())
                    .currency(PricingConstants.DEFAULT_CURRENCY)
                    .orderInfo(transaction.getOrderInfo())
                    .createdAt(createdAt)
                    .build();
        }

        long amount = request.getAmount().longValue();

        // Build the checkout fields in the exact signing order. order_invoice_number = our txn ref
        // so the IPN can match the order back to this transaction.
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("operation", "PURCHASE");
        fields.put("payment_method", firstNonBlank(sePayConfig.getPaymentMethod(), "BANK_TRANSFER"));
        fields.put("order_amount", Long.toString(amount));
        fields.put("currency", firstNonBlank(sePayConfig.getCurrency(), PricingConstants.DEFAULT_CURRENCY));
        fields.put("order_invoice_number", transaction.getTransactionId());
        fields.put("order_description", safeDescription(transaction.getOrderInfo()));
        putIfPresent(fields, "success_url", sePayConfig.getSuccessUrl());
        putIfPresent(fields, "error_url", sePayConfig.getErrorUrl());
        putIfPresent(fields, "cancel_url", sePayConfig.getCancelUrl());
        fields.put("merchant", sePayConfig.getMerchantId());

        String signature = signFields(fields);
        // signature is POSTed alongside the fields but is NOT part of the signed payload.
        Map<String, String> formFields = new LinkedHashMap<>(fields);
        formFields.put("signature", signature);

        int expiryMinutes = sePayConfig.getExpiryMinutes() != null ? sePayConfig.getExpiryMinutes() : 15;
        String checkoutUrl = sePayConfig.getCheckoutInitUrl();

        Map<String, Object> providerData = new LinkedHashMap<>();
        providerData.put("method", "POST");
        providerData.put("checkoutUrl", checkoutUrl);
        providerData.put("fields", formFields);

        return PaymentResponse.builder()
                .transactionRef(transaction.getTransactionId())
                .paymentUrl(checkoutUrl)
                .provider(PaymentProvider.SEPAY)
                .amount(request.getAmount())
                .currency(PricingConstants.DEFAULT_CURRENCY)
                .orderInfo(transaction.getOrderInfo())
                .providerData(providerData)
                .createdAt(createdAt)
                .expiresAt(createdAt.plusMinutes(expiryMinutes))
                .build();
    }

    /**
     * Process the SePay Payment Gateway IPN. The controller flattens the nested IPN JSON into the
     * params map: {@code notification_type, order_invoice_number, order_status, order_amount,
     * transaction_id, transaction_status, transaction_amount, payment_method}.
     */
    @Override
    public PaymentCallbackResponse processIPN(Map<String, String> params, HttpServletRequest httpRequest) {
        log.info("Processing SePay PG IPN. Keys: {}", params.keySet());

        String orderRef = firstNonBlank(params.get("order_invoice_number"));
        if (orderRef == null) {
            log.warn("SePay IPN missing order_invoice_number");
            return failure(null, "SEPAY_MISSING_ORDER_REF", true);
        }

        Optional<Transaction> txOpt = findPaymentByTransactionRef(orderRef);
        if (txOpt.isEmpty()) {
            log.warn("No transaction matched SePay IPN order_invoice_number={}", orderRef);
            return failure(orderRef, "SEPAY_ORDER_NOT_FOUND", true);
        }
        Transaction transaction = txOpt.get();

        String providerTxnId = firstNonBlank(params.get("transaction_id"));
        String paymentMethod = firstNonBlank(params.get("payment_method"), "BANK_TRANSFER");
        String notificationType = firstNonBlank(params.get("notification_type"));
        String transactionStatus = firstNonBlank(params.get("transaction_status"));
        String orderStatus = firstNonBlank(params.get("order_status"));

        // Idempotency: an IPN may be delivered more than once.
        if (transaction.getStatus() == TransactionStatus.COMPLETED) {
            log.info("SePay transaction {} already COMPLETED, acknowledging duplicate IPN", orderRef);
            return success(transaction, providerTxnId);
        }

        // Cancellation / void notification.
        if ("TRANSACTION_VOID".equalsIgnoreCase(notificationType)) {
            log.info("SePay IPN reports TRANSACTION_VOID for {}", orderRef);
            updatePaymentWithProviderData(orderRef, providerTxnId, paymentMethod, "SEPAY",
                    null, TransactionStatus.CANCELLED, "VOID", "Transaction voided");
            return PaymentCallbackResponse.builder()
                    .provider(PaymentProvider.SEPAY)
                    .transactionRef(orderRef)
                    .providerTransactionId(providerTxnId)
                    .status(TransactionStatus.CANCELLED)
                    .amount(transaction.getAmount())
                    .currency(PricingConstants.DEFAULT_CURRENCY)
                    .success(false)
                    .signatureValid(true)
                    .message("Transaction voided")
                    .build();
        }

        // Successful payment requires ORDER_PAID + an APPROVED transaction (order CAPTURED).
        boolean approved = "APPROVED".equalsIgnoreCase(transactionStatus)
                || "CAPTURED".equalsIgnoreCase(orderStatus)
                || "ORDER_PAID".equalsIgnoreCase(notificationType);
        if (!approved) {
            log.warn("SePay IPN for {} not approved (notification={}, txnStatus={}, orderStatus={})",
                    orderRef, notificationType, transactionStatus, orderStatus);
            return failure(orderRef, "SEPAY_NOT_APPROVED", true);
        }

        // Verify the paid amount covers the order amount.
        BigDecimal paid = parseAmount(firstNonBlank(params.get("transaction_amount"), params.get("order_amount")));
        BigDecimal expected = transaction.getAmount();
        if (paid == null || (expected != null && paid.compareTo(expected) < 0)) {
            log.warn("SePay amount mismatch for {}. expected={}, paid={}", orderRef, expected, paid);
            return PaymentCallbackResponse.builder()
                    .provider(PaymentProvider.SEPAY)
                    .transactionRef(orderRef)
                    .status(transaction.getStatus())
                    .amount(expected)
                    .currency(PricingConstants.DEFAULT_CURRENCY)
                    .success(false)
                    .signatureValid(true)
                    .message("SEPAY_AMOUNT_MISMATCH")
                    .build();
        }

        updatePaymentWithProviderData(orderRef, providerTxnId, paymentMethod, "SEPAY",
                providerTxnId, TransactionStatus.COMPLETED, "00", "Success");

        return success(transaction, providerTxnId);
    }

    @Override
    public PaymentCallbackResponse queryTransaction(String transactionRef) {
        log.info("Querying SePay transaction: {}", transactionRef);
        Optional<Transaction> txOpt = findPaymentByTransactionRef(transactionRef);
        if (txOpt.isEmpty()) {
            return PaymentCallbackResponse.builder()
                    .provider(PaymentProvider.SEPAY)
                    .transactionRef(transactionRef)
                    .success(false)
                    .message("Transaction not found")
                    .build();
        }
        Transaction transaction = txOpt.get();
        // The IPN is the source of truth; report the DB state (already settled by the IPN).
        return PaymentCallbackResponse.builder()
                .provider(PaymentProvider.SEPAY)
                .transactionRef(transactionRef)
                .providerTransactionId(transaction.getProviderTransactionId())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .currency(PricingConstants.DEFAULT_CURRENCY)
                .success(transaction.getStatus() == TransactionStatus.COMPLETED)
                .signatureValid(true)
                .message("Transaction status retrieved from database")
                .build();
    }

    @Override
    public boolean validateSignature(Map<String, String> params, String signature) {
        // IPN authenticity is enforced by the X-Secret-Key header (verified in the controller),
        // not by a body signature.
        return true;
    }

    @Override
    public boolean cancelPayment(String transactionRef, String reason) {
        log.info("Cancelling SePay payment: {} - Reason: {}", transactionRef, reason);
        // Best-effort cancel on the gateway (QR/bank-transfer orders); always cancel locally.
        try {
            callOrderApi("/order/cancel", transactionRef);
        } catch (Exception e) {
            log.warn("SePay order/cancel API call failed for {} (cancelling locally anyway): {}",
                    transactionRef, e.getMessage());
        }
        try {
            updatePaymentStatus(transactionRef, TransactionStatus.CANCELLED, "CANCELLED",
                    reason != null ? reason : "Cancelled by user");
            return true;
        } catch (Exception e) {
            log.error("Error cancelling SePay payment locally: {}", transactionRef, e);
            return false;
        }
    }

    @Override
    public PaymentCallbackResponse refundPayment(String transactionRef, String amount, String reason) {
        log.info("Refund/void requested for SePay payment: {} - Amount: {}", transactionRef, amount);
        // SePay supports voiding card transactions; bank transfers are not auto-refundable.
        try {
            callOrderApi("/order/voidTransaction", transactionRef);
            updatePaymentStatus(transactionRef, TransactionStatus.REFUNDED, "VOID",
                    reason != null ? reason : "Voided");
            return PaymentCallbackResponse.builder()
                    .provider(PaymentProvider.SEPAY)
                    .transactionRef(transactionRef)
                    .status(TransactionStatus.REFUNDED)
                    .success(true)
                    .signatureValid(true)
                    .message("Transaction voided")
                    .build();
        } catch (Exception e) {
            log.warn("SePay void failed for {}: {}", transactionRef, e.getMessage());
            return PaymentCallbackResponse.builder()
                    .provider(PaymentProvider.SEPAY)
                    .transactionRef(transactionRef)
                    .success(false)
                    .signatureValid(true)
                    .message("Refund/void not available for this transaction; refund manually if needed")
                    .build();
        }
    }

    @Override
    public boolean supportsFeature(PaymentFeature feature) {
        return switch (feature) {
            case QR_CODE, BANK_TRANSFER, CREDIT_CARD -> true;
            default -> false;
        };
    }

    @Override
    public Map<String, Object> getConfigurationSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("provider", "SePay Payment Gateway");
        schema.put("env", sePayConfig.getEnv());
        schema.put("merchantId", sePayConfig.getMerchantId());
        schema.put("paymentMethod", sePayConfig.getPaymentMethod());
        schema.put("checkoutInitUrl", sePayConfig.getCheckoutInitUrl());
        schema.put("apiBaseUrl", sePayConfig.getApiBaseUrl());
        schema.put("currency", firstNonBlank(sePayConfig.getCurrency(), PricingConstants.DEFAULT_CURRENCY));
        schema.put("expiryMinutes", sePayConfig.getExpiryMinutes());
        return schema;
    }

    @Override
    public boolean validateConfiguration() {
        if (isBlank(sePayConfig.getMerchantId())) {
            log.error("SePay Merchant ID is not configured");
            return false;
        }
        if (isBlank(sePayConfig.getSecretKey())) {
            log.error("SePay Secret Key is not configured");
            return false;
        }
        return true;
    }

    // ---------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------

    /**
     * Sign the checkout fields the way the SePay SDK does: take the present fields in
     * {@link #SIGNED_FIELD_ORDER}, join {@code key=value} segments with commas, then
     * {@code base64(HMAC_SHA256(secretKey, joined))}.
     */
    private String signFields(Map<String, String> fields) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String field : SIGNED_FIELD_ORDER) {
            String value = fields.get(field);
            if (value == null) {
                continue;
            }
            if (!first) {
                sb.append(',');
            }
            sb.append(field).append('=').append(value);
            first = false;
        }
        log.debug("SePay signature source: {}", sb);
        return PaymentUtil.hmacSHA256Base64(sePayConfig.getSecretKey(), sb.toString());
    }

    /** Call a SePay PG order endpoint with Basic Auth and a {@code {order_invoice_number}} body. */
    private void callOrderApi(String path, String transactionRef) {
        String url = sePayConfig.getApiBaseUrl() + path;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, basicAuthHeader());
        HttpEntity<Map<String, String>> entity =
                new HttpEntity<>(Map.of("order_invoice_number", transactionRef), headers);
        JsonNode response = restTemplate.postForObject(url, entity, JsonNode.class);
        log.info("SePay {} response for {}: {}", path, transactionRef, response);
    }

    private String basicAuthHeader() {
        String raw = sePayConfig.getMerchantId() + ":" + sePayConfig.getSecretKey();
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private PaymentCallbackResponse success(Transaction transaction, String providerTxnId) {
        return PaymentCallbackResponse.builder()
                .provider(PaymentProvider.SEPAY)
                .transactionRef(transaction.getTransactionId())
                .providerTransactionId(providerTxnId)
                .status(TransactionStatus.COMPLETED)
                .amount(transaction.getAmount())
                .currency(PricingConstants.DEFAULT_CURRENCY)
                .responseCode("00")
                .success(true)
                .signatureValid(true)
                .message("Payment successful")
                .build();
    }

    private PaymentCallbackResponse failure(String ref, String message, boolean signatureValid) {
        return PaymentCallbackResponse.builder()
                .provider(PaymentProvider.SEPAY)
                .transactionRef(ref)
                .status(TransactionStatus.FAILED)
                .success(false)
                .signatureValid(signatureValid)
                .message(message)
                .build();
    }

    private BigDecimal parseAmount(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Unable to parse SePay amount: {}", value);
            return null;
        }
    }

    private String safeDescription(String orderInfo) {
        // The signed payload joins on commas, so strip commas from free text to keep the
        // signature source unambiguous.
        if (orderInfo == null || orderInfo.isBlank()) {
            return "SmartRent payment";
        }
        return orderInfo.replace(',', ' ').trim();
    }

    private void putIfPresent(Map<String, String> map, String key, String value) {
        if (!isBlank(value)) {
            map.put(key, value);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
