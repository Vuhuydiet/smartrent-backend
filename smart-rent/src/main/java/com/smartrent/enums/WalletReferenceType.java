package com.smartrent.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum WalletReferenceType {
    PAYMENT("payment", "Reference to a payment transaction"),
    MANUAL("manual", "Manual operation by admin or system"),
    SYSTEM("system", "Automatic system operation"),
    REFUND("refund", "Reference to a refund transaction");

    String code;
    String description;
}