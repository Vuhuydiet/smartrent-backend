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
    VNPAY("vnpay", "VNPay Payment Gateway (legacy)"),

    /**
     * Legacy provider — PayOS was the default gateway before the SePay
     * migration (see V88/V89). Kept only so historical transactions created
     * during that window can still be read (@Enumerated(EnumType.STRING) on
     * Transaction.paymentProvider throws IllegalArgumentException on any DB
     * value with no matching constant, breaking GET /v1/me/transactions for
     * any user with an old PayOS transaction). No provider bean is registered
     * for it, so it must not be used to initiate new payments.
     */
    PAYOS("payos", "PayOS Payment Gateway (legacy)");

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
