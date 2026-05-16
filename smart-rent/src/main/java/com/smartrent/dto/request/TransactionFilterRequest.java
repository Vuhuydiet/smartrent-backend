package com.smartrent.dto.request;

import com.smartrent.enums.PaymentProvider;
import com.smartrent.enums.TransactionStatus;
import com.smartrent.enums.TransactionType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionFilterRequest {
    String customerId;
    String landlordId;
    TransactionStatus status;
    TransactionType type;
    PaymentProvider gateway;
    LocalDate fromDate;
    LocalDate toDate;
    String q;
}
