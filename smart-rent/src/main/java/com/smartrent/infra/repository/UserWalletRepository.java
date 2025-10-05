package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.UserWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserWalletRepository extends JpaRepository<UserWallet, Long> {

    /**
     * Find wallet by user ID
     */
    Optional<UserWallet> findByUserId(String userId);

    /**
     * Find active wallet by user ID
     */
    Optional<UserWallet> findByUserIdAndIsActiveTrue(String userId);

    /**
     * Check if user has an active wallet
     */
    boolean existsByUserIdAndIsActiveTrue(String userId);

    /**
     * Find wallets by currency
     */
    List<UserWallet> findByCurrency(String currency);

    /**
     * Find wallets with balance greater than specified amount
     */
    @Query("SELECT w FROM user_wallets w WHERE w.creditBalance > :amount AND w.isActive = true")
    List<UserWallet> findActiveWalletsWithBalanceGreaterThan(@Param("amount") BigDecimal amount);

    /**
     * Find wallets with zero balance
     */
    @Query("SELECT w FROM user_wallets w WHERE w.creditBalance = 0 AND w.isActive = true")
    List<UserWallet> findActiveWalletsWithZeroBalance();

    /**
     * Count active wallets
     */
    long countByIsActiveTrue();

    /**
     * Get total balance across all active wallets
     */
    @Query("SELECT COALESCE(SUM(w.creditBalance), 0) FROM user_wallets w WHERE w.isActive = true")
    BigDecimal getTotalActiveBalance();

    /**
     * Get total balance for specific currency
     */
    @Query("SELECT COALESCE(SUM(w.creditBalance), 0) FROM user_wallets w WHERE w.currency = :currency AND w.isActive = true")
    BigDecimal getTotalActiveBalanceByCurrency(@Param("currency") String currency);

    /**
     * Deactivate wallet
     */
    @Query("UPDATE user_wallets w SET w.isActive = false WHERE w.userId = :userId")
    void deactivateWalletByUserId(@Param("userId") String userId);
}