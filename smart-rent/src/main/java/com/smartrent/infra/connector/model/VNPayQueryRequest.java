package com.smartrent.infra.connector.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VNPayQueryRequest {
    String vnp_RequestId;
    String vnp_Version;
    String vnp_Command;
    String vnp_TmnCode;
    String vnp_TxnRef;
    String vnp_OrderInfo;
    String vnp_TransactionDate;
    String vnp_CreateDate;
    String vnp_IpAddr;
    String vnp_SecureHash;
}
