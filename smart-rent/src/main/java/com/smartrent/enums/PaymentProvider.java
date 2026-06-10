package com.smartrent.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum PaymentProvider {
    SEPAY("sepay", "SePay Payment Gateway"),
    PAYPAL("paypal", "PayPal Payment Gateway"),
    MOMO("momo", "MoMo Payment Gateway"),
    ZALOPAY("zalopay", "ZaloPay Payment Gateway"),

    /**
     * Legacy provider, kept only so historical transactions created before the
     * SePay migration can still be read. No provider bean is registered for it,
     * so it must not be used to initiate new payments.
     */
    VNPAY("vnpay", "VNPay Payment Gateway (legacy)");

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
