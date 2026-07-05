package com.smartrent.dto.request;

import com.smartrent.enums.PaymentProvider;
import com.smartrent.enums.TransactionStatus;
import com.smartrent.enums.TransactionType;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionFilterRequest {
    String customerId;
    String landlordId;
    String transactionId;
    /** Free-text match against customer name or phone (snapshot columns on Transaction). */
    String customer;
    TransactionStatus status;
    TransactionType paymentType;
    PaymentProvider paymentGateway;
    /** Single date ("2026-02-09") or a range ("2026-02-09..2026-03-10"); either side of a range may be omitted. */
    String createdAt;
    String q;
}
