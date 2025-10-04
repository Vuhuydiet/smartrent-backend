package com.smartrent.service.payment.provider.vnpay;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum VNPayTransactionType {
    PAYMENT("payment", "Payment transaction"),
    REFUND("refund", "Refund transaction"),
    QUERY("query", "Query transaction status");

    String code;
    String description;
}
