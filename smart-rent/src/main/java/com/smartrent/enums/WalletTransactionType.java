package com.smartrent.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum WalletTransactionType {
    CREDIT_ADD("credit_add", "Add credit to wallet"),
    CREDIT_SUBTRACT("credit_subtract", "Subtract credit from wallet"),
    PAYMENT_CREDIT("payment_credit", "Credit added from successful payment"),
    REFUND_CREDIT("refund_credit", "Credit added from refund"),
    ADJUSTMENT("adjustment", "Manual adjustment by admin");

    String code;
    String description;

    public boolean isCredit() {
        return this == CREDIT_ADD || this == PAYMENT_CREDIT || this == REFUND_CREDIT || this == ADJUSTMENT;
    }

    public boolean isDebit() {
        return this == CREDIT_SUBTRACT;
    }
}