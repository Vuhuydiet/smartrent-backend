package com.smartrent.service.payment.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrent.config.ZaloPayConfig;
import com.smartrent.constants.PricingConstants;
import com.smartrent.dto.request.PaymentRequest;
import com.smartrent.dto.response.PaymentCallbackResponse;
import com.smartrent.dto.response.PaymentResponse;
import com.smartrent.enums.PaymentProvider;
import com.smartrent.enums.TransactionStatus;
import com.smartrent.infra.repository.PaymentRepository;
import com.smartrent.infra.repository.entity.Transaction;
import com.smartrent.utility.PaymentUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class ZaloPayPaymentProvider extends AbstractPaymentProvider {

    private final ZaloPayConfig zaloPayConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final java.time.ZoneId VIETNAM_ZONE = java.time.ZoneId.of("Asia/Ho_Chi_Minh");

    public ZaloPayPaymentProvider(PaymentRepository paymentRepository, 
                                 ZaloPayConfig zaloPayConfig, 
                                 RestTemplate restTemplate, 
                                 ObjectMapper objectMapper) {
        super(paymentRepository);
        this.zaloPayConfig = zaloPayConfig;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public PaymentResponse createPayment(PaymentRequest request, HttpServletRequest httpRequest) {
        log.info("Creating ZaloPay payment for request: {}", request.getOrderInfo());
        
        // Validate configuration
        if (zaloPayConfig.getAppId() == null || zaloPayConfig.getKey1() == null || zaloPayConfig.getKey2() == null) {
            log.error("ZaloPay configuration is missing required keys: appId={}, key1={}, key2={}", 
                    zaloPayConfig.getAppId() != null ? "FOUND" : "MISSING",
                    zaloPayConfig.getKey1() != null ? "FOUND" : "MISSING",
                    zaloPayConfig.getKey2() != null ? "FOUND" : "MISSING");
            throw new RuntimeException("ZaloPay configuration is incomplete. Please check your .env or application.yml");
        }
        
        // Use createPaymentRecord from AbstractPaymentProvider
        Transaction transaction = createPaymentRecord(request, httpRequest);

        try {
            String appTime = String.valueOf(System.currentTimeMillis());
            // ZaloPay app_trans_id must be <= 40 characters. 
            // Format: yyMMdd_uniqueId. UUID is 36 chars + 7 chars for yyMMdd_ = 43 total (too long).
            String shortId = transaction.getTransactionId().length() > 20 
                             ? transaction.getTransactionId().substring(0, 20) 
                             : transaction.getTransactionId();
            String appTransId = LocalDateTime.now(VIETNAM_ZONE).format(DateTimeFormatter.ofPattern("yyMMdd")) 
                                + "_" + shortId;
            
            log.info("Generated appTransId: {} (length: {})", appTransId, appTransId.length());
            
            // Store app_trans_id for lookup in additional_info
            String currentInfo = transaction.getAdditionalInfo() != null ? transaction.getAdditionalInfo() : "";
            transaction.setAdditionalInfo(currentInfo + " | zalo_app_trans_id=" + appTransId);
            paymentRepository.save(transaction);

            long amount = request.getAmount().longValue();
            String appUser = transaction.getUserId() != null ? transaction.getUserId() : "guest";
            String item = "[]";
            
            Map<String, String> embedDataMap = new HashMap<>();
            String redirectUrl = firstNonBlank(request.getReturnUrl(), zaloPayConfig.getReturnUrl());
            if (redirectUrl != null) {
                embedDataMap.put("redirecturl", redirectUrl);
            }
            String embedDataStr = "{}";
            try {
                embedDataStr = objectMapper.writeValueAsString(embedDataMap);
            } catch (Exception e) {}
            
            String description = "Thanh toan don hang " + appTransId;
            
            // MAC formula: app_id|app_trans_id|app_user|amount|app_time|embed_data|item
            String dataForMac = zaloPayConfig.getAppId() + "|" + appTransId + "|" + appUser + "|" + amount + "|" 
                                + appTime + "|" + embedDataStr + "|" + item;
            
            log.info("ZaloPay MAC source: {}", dataForMac);
            String mac = PaymentUtil.hmacSHA256(zaloPayConfig.getKey1(), dataForMac);

            Map<String, Object> orderReq = new HashMap<>();
            orderReq.put("app_id", Integer.parseInt(zaloPayConfig.getAppId()));
            orderReq.put("app_trans_id", appTransId);
            orderReq.put("app_user", appUser);
            orderReq.put("app_time", Long.parseLong(appTime));
            orderReq.put("item", item);
            orderReq.put("embed_data", embedDataStr);
            orderReq.put("amount", amount);
            orderReq.put("description", description);
            orderReq.put("bank_code", "");
            orderReq.put("mac", mac);
            if (zaloPayConfig.getIpnUrl() != null) {
                orderReq.put("callback_url", zaloPayConfig.getIpnUrl());
            }

            log.info("Sending request to ZaloPay create order API: {}", zaloPayConfig.getCreateOrderUrl());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(orderReq, headers);

            JsonNode jsonResponse = restTemplate.postForObject(zaloPayConfig.getCreateOrderUrl(), entity, JsonNode.class);
            log.debug("ZaloPay response: {}", jsonResponse);

            int returnCode = jsonResponse.path("return_code").asInt();
            if (returnCode == 1) {
                String orderUrl = jsonResponse.path("order_url").asText();
                LocalDateTime createdAt = transaction.getCreatedAt() != null ? transaction.getCreatedAt() : LocalDateTime.now(VIETNAM_ZONE);
                return PaymentResponse.builder()
                        .transactionRef(transaction.getTransactionId())
                        .paymentUrl(orderUrl)
                        .provider(PaymentProvider.ZALOPAY)
                        .amount(request.getAmount())
                        .currency(PricingConstants.DEFAULT_CURRENCY)
                        .createdAt(createdAt)
                        .expiresAt(createdAt.plusMinutes(zaloPayConfig.getTimeoutMinutes()))
                        .build();
            } else {
                String message = jsonResponse.path("return_message").asText("Unknown error");
                log.error("ZaloPay create logic failed with return_code {}: {}", returnCode, message);
                throw new RuntimeException("ZaloPay returned error: " + message);
            }
        } catch (Exception e) {
            log.error("Failed to create ZaloPay order", e);
            throw new RuntimeException("Error during ZaloPay communication", e);
        }
    }

    @Override
    public PaymentCallbackResponse processIPN(Map<String, String> params, HttpServletRequest httpRequest) {
        log.info("Processing ZaloPay IPN/Callback with params: {}", params);

        String status = firstNonBlank(params.get("status"), params.get("resultCode"));
        String appTransId = firstNonBlank(
                params.get("apptransid"),
                params.get("app_trans_id"),
                params.get("orderId")
        );

        if (appTransId == null) {
            return PaymentCallbackResponse.builder()
                    .provider(PaymentProvider.ZALOPAY)
                    .status(TransactionStatus.FAILED)
                    .success(false)
                    .signatureValid(true)
                    .message("ZALOPAY_MISSING_APPTRANSID")
                    .build();
        }

        Optional<Transaction> txOpt = paymentRepository.findByAdditionalInfoContaining("zalo_app_trans_id=" + appTransId);

        if (txOpt.isEmpty()) {
            return PaymentCallbackResponse.builder()
                    .provider(PaymentProvider.ZALOPAY)
                    .status(TransactionStatus.FAILED)
                    .success(false)
                    .signatureValid(true)
                    .message("ZALOPAY_ORDER_NOT_FOUND")
                    .build();
        }

        if (status == null) {
            return PaymentCallbackResponse.builder()
                    .provider(PaymentProvider.ZALOPAY)
                    .transactionRef(txOpt.get().getTransactionId())
                    .status(TransactionStatus.FAILED)
                    .success(false)
                    .signatureValid(true)
                    .message("ZALOPAY_MISSING_STATUS")
                    .build();
        }

        Transaction transaction = txOpt.get();
        String normalizedStatus = status.trim().toUpperCase();
        boolean success = "1".equals(normalizedStatus) || "00".equals(normalizedStatus) || "SUCCESS".equals(normalizedStatus);
        boolean cancelled = "2".equals(normalizedStatus) || "CANCELLED".equals(normalizedStatus) || "CANCELED".equals(normalizedStatus);
        TransactionStatus txStatus = success
                ? TransactionStatus.COMPLETED
                : (cancelled ? TransactionStatus.CANCELLED : TransactionStatus.FAILED);

        String providerTxnId = firstNonBlank(params.get("zp_trans_id"), params.get("zptranstoken"));
        String paymentMethod = firstNonBlank(params.get("pmcid"), "ZALOPAY");
        String bankCode = firstNonBlank(params.get("bankcode"), "ZALOPAY");
        String bankTransactionId = firstNonBlank(params.get("zp_trans_id"), params.get("banktransid"));
        String checksum = firstNonBlank(params.get("checksum"), params.get("mac"));

        updatePaymentWithProviderData(
                transaction.getTransactionId(),
                providerTxnId,
                paymentMethod,
                bankCode,
                bankTransactionId,
                txStatus,
                normalizedStatus,
                success ? "Success" : (cancelled ? "Cancelled" : "Failed")
        );

        return PaymentCallbackResponse.builder()
                .provider(PaymentProvider.ZALOPAY)
                .transactionRef(transaction.getTransactionId())
                .providerTransactionId(providerTxnId)
                .status(txStatus)
                .success(success)
                .signatureValid(validateSignature(params, checksum))
                .responseCode(normalizedStatus)
                .message(success ? "Success" : (cancelled ? "Cancelled" : "Failed"))
                .build();
    }

    @Override
    public PaymentCallbackResponse queryTransaction(String transactionRef) {
        log.info("Querying ZaloPay transaction: {}", transactionRef);
        return PaymentCallbackResponse.builder()
                .transactionRef(transactionRef)
                .message("Query not fully implemented yet")
                .success(false)
                .build();
    }

    @Override
    public boolean validateSignature(Map<String, String> params, String signature) {
        // Simple validation for redirect - in production should verify checksum
        log.info("Validating ZaloPay signature: {}", signature);
        return true;
    }

    @Override
    public boolean cancelPayment(String transactionId, String reason) {
        log.info("Cancelling ZaloPay payment: {} - Reason: {}", transactionId, reason);
        return false;
    }

    @Override
    public PaymentCallbackResponse refundPayment(String transactionRef, String amount, String reason) {
        log.info("Refunding ZaloPay payment: {} - Amount: {}", transactionRef, amount);
        return PaymentCallbackResponse.builder()
                .transactionRef(transactionRef)
                .message("Refund not implemented for ZaloPay yet")
                .success(false)
                .build();
    }

    @Override
    public Map<String, Object> getConfigurationSchema() {
        return new HashMap<>();
    }

    @Override
    public PaymentProvider getProviderType() {
        return PaymentProvider.ZALOPAY;
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
