package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity(name = "vnpay_payment_details")
@Table(name = "vnpay_payment_details",
        indexes = {
                @Index(name = "idx_vnpay_payment_id", columnList = "payment_id"),
                @Index(name = "idx_vnpay_txn_ref", columnList = "vnp_txn_ref"),
                @Index(name = "idx_vnpay_transaction_no", columnList = "vnp_transaction_no"),
                @Index(name = "idx_vnpay_response_code", columnList = "vnp_response_code")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VNPayPaymentDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "payment_id", nullable = false, unique = true)
    Long paymentId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", insertable = false, updatable = false)
    Payment payment;

    @Column(name = "vnp_txn_ref", nullable = false, length = 100)
    String vnpTxnRef;

    @Column(name = "vnp_order_info", length = 500)
    String vnpOrderInfo;

    @Column(name = "vnp_amount", nullable = false)
    Long vnpAmount;

    @Column(name = "vnp_order_type", length = 20)
    String vnpOrderType;

    @Column(name = "vnp_locale", length = 5)
    String vnpLocale;

    @Column(name = "vnp_bank_code", length = 20)
    String vnpBankCode;

    @Column(name = "vnp_bank_tran_no", length = 100)
    String vnpBankTranNo;

    @Column(name = "vnp_card_type", length = 20)
    String vnpCardType;

    @Column(name = "vnp_pay_date", length = 14)
    String vnpPayDate;

    @Column(name = "vnp_response_code", length = 10)
    String vnpResponseCode;

    @Column(name = "vnp_tmn_code", length = 20)
    String vnpTmnCode;

    @Column(name = "vnp_transaction_no", length = 100)
    String vnpTransactionNo;

    @Column(name = "vnp_transaction_status", length = 10)
    String vnpTransactionStatus;

    @Column(name = "vnp_secure_hash", length = 256)
    String vnpSecureHash;

    @Column(name = "vnp_secure_hash_type", length = 10)
    String vnpSecureHashType;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    LocalDateTime updatedAt;

    // Helper methods
    public boolean isSuccessful() {
        return "00".equals(vnpResponseCode);
    }

    public boolean isFailed() {
        return vnpResponseCode != null && !"00".equals(vnpResponseCode);
    }
}
