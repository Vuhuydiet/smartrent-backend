package com.smartrent.mapper;

import com.smartrent.dto.response.PaymentHistoryResponse;
import com.smartrent.infra.repository.entity.Payment;

public interface PaymentMapper {
    PaymentHistoryResponse toPaymentHistoryResponse(Payment payment);
}
