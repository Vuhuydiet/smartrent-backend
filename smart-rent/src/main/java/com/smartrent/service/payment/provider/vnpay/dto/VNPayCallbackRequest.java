package com.smartrent.service.payment.provider.vnpay.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "VNPay callback/return request parameters")
public class VNPayCallbackRequest {

    @Schema(description = "Amount in VND cents", example = "10000000")
    String vnp_Amount;

    @Schema(description = "Bank code", example = "NCB")
    String vnp_BankCode;

    @Schema(description = "Bank transaction ID", example = "VNP13863934")
    String vnp_BankTranNo;

    @Schema(description = "Card type", example = "ATM")
    String vnp_CardType;

    @Schema(description = "Order info", example = "Payment for listing rental")
    String vnp_OrderInfo;

    @Schema(description = "Payment date", example = "20231215140530")
    String vnp_PayDate;

    @Schema(description = "Response code", example = "00")
    String vnp_ResponseCode;

    @Schema(description = "Terminal code", example = "2QXUI4J4")
    String vnp_TmnCode;

    @Schema(description = "Transaction no", example = "13863934")
    String vnp_TransactionNo;

    @Schema(description = "Transaction reference", example = "12345678")
    String vnp_TxnRef;

    @Schema(description = "Secure hash", example = "abc123def456...")
    String vnp_SecureHash;

    @Schema(description = "Transaction status", example = "00")
    String vnp_TransactionStatus;

    @Schema(description = "Version", example = "2.1.0")
    String vnp_Version;

    @Schema(description = "Command", example = "pay")
    String vnp_Command;

    @Schema(description = "Currency code", example = "VND")
    String vnp_CurrCode;

    @Schema(description = "Locale", example = "vn")
    String vnp_Locale;

    @Schema(description = "Create date", example = "20231215140000")
    String vnp_CreateDate;

    @Schema(description = "IP address", example = "192.168.1.1")
    String vnp_IpAddr;
}
