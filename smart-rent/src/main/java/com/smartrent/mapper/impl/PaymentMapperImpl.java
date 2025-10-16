package com.smartrent.mapper.impl;

import com.smartrent.dto.response.PaymentHistoryResponse;
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
                .status(transaction.getStatus())
                .orderInfo(transaction.getAdditionalInfo())
                .paymentMethod(transaction.getPaymentProvider() != null ? transaction.getPaymentProvider().name() : null)
                .paymentDate(transaction.isCompleted() ? transaction.getUpdatedAt() : null)
                .userId(transaction.getUserId() != null ? Long.parseLong(transaction.getUserId()) : null)
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .notes(transaction.getAdditionalInfo())
                .build();
    }
}

