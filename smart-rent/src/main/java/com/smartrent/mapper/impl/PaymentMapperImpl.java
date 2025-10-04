package com.smartrent.mapper.impl;

import com.smartrent.dto.response.PaymentHistoryResponse;
import com.smartrent.infra.repository.entity.Payment;
import com.smartrent.mapper.PaymentMapper;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapperImpl implements PaymentMapper {

    @Override
    public PaymentHistoryResponse toPaymentHistoryResponse(Payment payment) {
        return PaymentHistoryResponse.builder()
                .paymentId(payment.getId())
                .transactionRef(payment.getTransactionRef())
                .providerTransactionId(payment.getProviderTransactionId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .transactionType(payment.getTransactionType())
                .status(payment.getStatus())
                .orderInfo(payment.getOrderInfo())
                .paymentMethod(payment.getPaymentMethod())
                .paymentDate(payment.getPaymentDate())
                .listingId(payment.getListingId())
                .userId(payment.getUserId())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .notes(payment.getNotes())
                .build();
    }
}
