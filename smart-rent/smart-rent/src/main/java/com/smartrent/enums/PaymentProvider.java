package com.smartrent.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum PaymentProvider {
    VNPAY("vnpay", "VNPay Payment Gateway"),
    PAYPAL("paypal", "PayPal Payment Gateway"),
    MOMO("momo", "MoMo Payment Gateway");

    String code;
    String displayName;

    public static PaymentProvider fromCode(String code) {
        for (PaymentProvider provider : values()) {
            if (provider.code.equals(code)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unknown payment provider: " + code);
    }
}
