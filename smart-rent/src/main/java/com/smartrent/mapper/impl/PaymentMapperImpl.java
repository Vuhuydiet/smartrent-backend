package com.smartrent.mapper.impl;

import com.smartrent.dto.response.PaymentHistoryResponse;
import com.smartrent.enums.TransactionStatus;
import com.smartrent.infra.repository.entity.Transaction;
import com.smartrent.mapper.PaymentMapper;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapperImpl implements PaymentMapper {

    @Override
    public PaymentHistoryResponse toPaymentHistoryResponse(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        return PaymentHistoryResponse.builder()
                .transactionRef(transaction.getTransactionId())
                .providerTransactionId(transaction.getProviderTransactionId())
                .amount(transaction.getAmount())
                .currency("VND") // Default currency
                .transactionType(transaction.getTransactionType())
                .status(toApiStatus(transaction.getStatus()))
                .orderInfo(transaction.getAdditionalInfo())
                .paymentMethod(transaction.getPaymentProvider() != null ? transaction.getPaymentProvider().name() : null)
                .paymentDate(transaction.isCompleted() ? transaction.getUpdatedAt() : null)
                .userId(transaction.getUserId())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .notes(transaction.getAdditionalInfo())
                .build();
    }

    /**
     * Expose the internal {@link TransactionStatus} with the vocabulary the frontend expects:
     * COMPLETED is surfaced as SUCCESS; every other status keeps its enum name
     * (PENDING/FAILED/CANCELLED/REFUNDED). Mirrors
     * {@code TransactionHistoryServiceImpl.toApiStatus} so /v1/payments/history and
     * /v1/me/transactions report the same status strings.
     */
    private String toApiStatus(TransactionStatus status) {
        if (status == null) {
            return null;
        }
        return status == TransactionStatus.COMPLETED ? "SUCCESS" : status.name();
    }
}

