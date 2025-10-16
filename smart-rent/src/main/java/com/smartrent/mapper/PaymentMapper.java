package com.smartrent.mapper;

import com.smartrent.dto.response.PaymentHistoryResponse;
import com.smartrent.infra.repository.entity.Transaction;

/**
 * Mapper interface for Payment/Transaction entities
 */
public interface PaymentMapper {

    /**
     * Convert Transaction entity to PaymentHistoryResponse
     */
    PaymentHistoryResponse toPaymentHistoryResponse(Transaction transaction);
}

