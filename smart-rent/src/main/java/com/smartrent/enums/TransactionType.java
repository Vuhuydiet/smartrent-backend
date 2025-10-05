package com.smartrent.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum TransactionType {
    PAYMENT("payment", "Payment transaction"),
    REFUND("refund", "Refund transaction"),
    QUERY("query", "Query transaction status"),
    CREDIT_ADD("credit_add", "Add credit to user wallet"),
    CREDIT_SUBTRACT("credit_subtract", "Subtract credit from user wallet");

    String code;
    String description;
}
