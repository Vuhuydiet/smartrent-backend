package com.smartrent.service.payment.impl;

import com.smartrent.dto.request.PaymentCallbackRequest;
import com.smartrent.dto.request.PaymentHistoryByStatusRequest;
import com.smartrent.dto.request.PaymentRefundRequest;
import com.smartrent.dto.request.PaymentRequest;
import com.smartrent.dto.request.PaymentStatusUpdateRequest;
import com.smartrent.dto.response.PaymentCallbackResponse;
import com.smartrent.dto.response.PaymentHistoryResponse;
import com.smartrent.dto.response.PaymentResponse;
import com.smartrent.mapper.PaymentMapper;
import com.smartrent.enums.PaymentProvider;
import com.smartrent.infra.repository.PaymentRepository;
import com.smartrent.infra.repository.entity.Payment;
import com.smartrent.service.payment.PaymentService;
import com.smartrent.service.payment.provider.PaymentProviderFactory;
import com.smartrent.service.payment.provider.PaymentProvider.PaymentFeature;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentServiceImpl implements PaymentService {

    PaymentProviderFactory paymentProviderFactory;
    PaymentRepository paymentRepository;
    PaymentMapper paymentMapper;

    // Generic Payment Methods (Provider-agnostic)

    @Override
    @Transactional
    public PaymentResponse createPayment(PaymentRequest request, HttpServletRequest httpRequest) {
        log.info("Creating payment for provider: {} with amount: {}", request.getProvider(), request.getAmount());

        try {
            com.smartrent.service.payment.provider.PaymentProvider provider =
                    paymentProviderFactory.getProvider(request.getProvider());

            return provider.createPayment(request, httpRequest);
        } catch (Exception e) {
            log.error("Error creating payment for provider: {}", request.getProvider(), e);
            throw new RuntimeException("Failed to create payment", e);
        }
    }

    @Override
    @Transactional
    public PaymentCallbackResponse processCallback(PaymentCallbackRequest request, HttpServletRequest httpRequest) {
        String txnRef = extractTransactionRef(request.getParams(), request.getProvider());
        log.info("Processing callback for provider: {} and transaction: {}", request.getProvider(), txnRef);

        try {
            com.smartrent.service.payment.provider.PaymentProvider provider =
                    paymentProviderFactory.getProvider(request.getProvider());

            return provider.processCallback(request.getParams(), httpRequest);
        } catch (Exception e) {
            log.error("Error processing callback for provider: {}", request.getProvider(), e);
            throw new RuntimeException("Failed to process callback", e);
        }
    }

    @Override
    @Transactional
    public PaymentCallbackResponse processIPN(PaymentCallbackRequest request, HttpServletRequest httpRequest) {
        String txnRef = extractTransactionRef(request.getParams(), request.getProvider());
        log.info("Processing IPN for provider: {} and transaction: {}", request.getProvider(), txnRef);

        try {
            com.smartrent.service.payment.provider.PaymentProvider provider =
                    paymentProviderFactory.getProvider(request.getProvider());

            return provider.processIPN(request.getParams(), httpRequest);
        } catch (Exception e) {
            log.error("Error processing IPN for provider: {}", request.getProvider(), e);
            throw new RuntimeException("Failed to process IPN", e);
        }
    }

    @Override
    public PaymentCallbackResponse queryTransaction(String transactionRef) {
        log.info("Querying transaction: {}", transactionRef);

        try {
            PaymentProvider providerType = determineProviderFromTransactionRef(transactionRef);
            com.smartrent.service.payment.provider.PaymentProvider provider =
                    paymentProviderFactory.getProvider(providerType);

            return provider.queryTransaction(transactionRef);
        } catch (Exception e) {
            log.error("Error querying transaction: {}", transactionRef, e);
            throw new RuntimeException("Failed to query transaction", e);
        }
    }

    @Override
    public PaymentCallbackResponse refundPayment(PaymentRefundRequest request) {
        log.info("Refunding payment: {} with amount: {}", request.getTransactionRef(), request.getAmount());

        try {
            PaymentProvider providerType = determineProviderFromTransactionRef(request.getTransactionRef());
            com.smartrent.service.payment.provider.PaymentProvider provider =
                    paymentProviderFactory.getProvider(providerType);

            return provider.refundPayment(request.getTransactionRef(), request.getAmount(), request.getReason());
        } catch (Exception e) {
            log.error("Error refunding payment: {}", request.getTransactionRef(), e);
            throw new RuntimeException("Failed to refund payment", e);
        }
    }


    // Payment Management Methods

    @Override
    public Payment getPaymentByTransactionRef(String transactionRef) {
        return paymentRepository.findByTransactionRef(transactionRef)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + transactionRef));
    }

    @Override
    public Page<PaymentHistoryResponse> getPaymentHistory(Long userId, Pageable pageable) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(paymentMapper::toPaymentHistoryResponse);
    }

    @Override
    public Page<PaymentHistoryResponse> getPaymentHistoryByStatus(PaymentHistoryByStatusRequest request) {
        return paymentRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                request.getUserId(), request.getStatus(), request.getPageable())
                .map(paymentMapper::toPaymentHistoryResponse);
    }

    @Override
    @Transactional
    public boolean cancelPayment(String transactionRef, String reason) {
        log.info("Cancelling payment: {}", transactionRef);

        try {
            PaymentProvider providerType = determineProviderFromTransactionRef(transactionRef);
            com.smartrent.service.payment.provider.PaymentProvider provider =
                    paymentProviderFactory.getProvider(providerType);

            return provider.cancelPayment(transactionRef, reason);
        } catch (Exception e) {
            log.error("Error cancelling payment: {}", transactionRef, e);
            return false;
        }
    }

    @Override
    public boolean transactionRefExists(String transactionRef) {
        return paymentRepository.existsByTransactionRef(transactionRef);
    }

    @Override
    @Transactional
    public Payment updatePaymentStatus(PaymentStatusUpdateRequest request) {
        Payment payment = getPaymentByTransactionRef(request.getTransactionRef());
        payment.setStatus(request.getStatus());
        if (request.getStatus().isSuccess()) {
            payment.setPaymentDate(java.time.LocalDateTime.now());
        }
        return paymentRepository.save(payment);
    }

    // Provider Management Methods

    @Override
    public List<PaymentProvider> getAvailableProviders() {
        return paymentProviderFactory.getAllProviders().stream()
                .map(com.smartrent.service.payment.provider.PaymentProvider::getProviderType)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentProvider> getProvidersByFeature(PaymentFeature feature) {
        return paymentProviderFactory.getProvidersByFeature(feature).stream()
                .map(com.smartrent.service.payment.provider.PaymentProvider::getProviderType)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isProviderAvailable(PaymentProvider provider) {
        return paymentProviderFactory.isProviderAvailable(provider);
    }

    @Override
    public Map<PaymentProvider, Map<String, Object>> getProviderSchemas() {
        return paymentProviderFactory.getProviderSchemas();
    }

    // Private helper methods

    private PaymentProvider determineProviderFromTransactionRef(String transactionRef) {
        if (transactionRef.startsWith("VNPAY_")) {
            return PaymentProvider.VNPAY;
        } else if (transactionRef.startsWith("PAYPAL_")) {
            return PaymentProvider.PAYPAL;
        } else if (transactionRef.startsWith("MOMO_")) {
            return PaymentProvider.MOMO;
        }

        // Fallback: check database for payment record
        paymentRepository.findByTransactionRef(transactionRef)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + transactionRef));

        // Default to VNPay if no prefix found (for backward compatibility)
        return PaymentProvider.VNPAY;
    }

    private String extractTransactionRef(Map<String, String> params, PaymentProvider providerType) {
        return switch (providerType) {
            case VNPAY -> params.get("vnp_TxnRef");
            case PAYPAL -> params.get("token");
            case MOMO -> params.get("orderId");
        };
    }


}