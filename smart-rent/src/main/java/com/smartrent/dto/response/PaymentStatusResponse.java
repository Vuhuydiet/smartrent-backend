package com.smartrent.dto.response;

import com.smartrent.enums.TransactionStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for payment status
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentStatusResponse {

    String transactionId;
    
    TransactionStatus status;
    
    BigDecimal amount;
    
    String orderInfo;
    
    String providerTransactionId;
    
    LocalDateTime createdAt;
    
    LocalDateTime updatedAt;
    
    Boolean success;
    
    String message;
}

