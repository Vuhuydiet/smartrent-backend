package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.VNPayPaymentDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VNPayPaymentDetailsRepository extends JpaRepository<VNPayPaymentDetails, Long> {

    /**
     * Find VNPay details by payment ID
     */
    Optional<VNPayPaymentDetails> findByPaymentId(Long paymentId);

    /**
     * Find VNPay details by VNPay transaction reference
     */
    Optional<VNPayPaymentDetails> findByVnpTxnRef(String vnpTxnRef);

    /**
     * Find VNPay details by VNPay transaction number
     */
    Optional<VNPayPaymentDetails> findByVnpTransactionNo(String vnpTransactionNo);

    /**
     * Check if VNPay transaction reference exists
     */
    boolean existsByVnpTxnRef(String vnpTxnRef);

    /**
     * Check if VNPay transaction number exists
     */
    boolean existsByVnpTransactionNo(String vnpTransactionNo);

    /**
     * Find successful VNPay payments by response code
     */
    @Query("SELECT v FROM vnpay_payment_details v WHERE v.vnpResponseCode = '00'")
    java.util.List<VNPayPaymentDetails> findSuccessfulPayments();

    /**
     * Find failed VNPay payments by response code
     */
    @Query("SELECT v FROM vnpay_payment_details v WHERE v.vnpResponseCode != '00' AND v.vnpResponseCode IS NOT NULL")
    java.util.List<VNPayPaymentDetails> findFailedPayments();

    /**
     * Find VNPay details by response code
     */
    java.util.List<VNPayPaymentDetails> findByVnpResponseCode(String responseCode);
}
