package com.smartrent.validator;

import com.smartrent.constants.PricingConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Validator for payment amounts
 * Ensures amounts match pricing constants
 */
@Slf4j
@Component
public class PaymentAmountValidator {

    /**
     * Validate Silver post payment amount
     */
    public boolean validateSilverPostAmount(BigDecimal amount, int durationDays) {
        BigDecimal expectedAmount = PricingConstants.calculateSilverPostPrice(durationDays);
        boolean isValid = amount.compareTo(expectedAmount) == 0;

        if (!isValid) {
            log.warn("Invalid Silver post amount. Expected: {}, Got: {}", expectedAmount, amount);
        }

        return isValid;
    }

    /**
     * Validate Gold post payment amount
     */
    public boolean validateGoldPostAmount(BigDecimal amount, int durationDays) {
        BigDecimal expectedAmount = PricingConstants.calculateGoldPostPrice(durationDays);
        boolean isValid = amount.compareTo(expectedAmount) == 0;

        if (!isValid) {
            log.warn("Invalid Gold post amount. Expected: {}, Got: {}", expectedAmount, amount);
        }

        return isValid;
    }

    /**
     * Validate Diamond post payment amount
     */
    public boolean validateDiamondPostAmount(BigDecimal amount, int durationDays) {
        BigDecimal expectedAmount = PricingConstants.calculateDiamondPostPrice(durationDays);
        boolean isValid = amount.compareTo(expectedAmount) == 0;

        if (!isValid) {
            log.warn("Invalid Diamond post amount. Expected: {}, Got: {}", expectedAmount, amount);
        }

        return isValid;
    }

    /**
     * Validate push payment amount
     */
    public boolean validatePushAmount(BigDecimal amount) {
        boolean isValid = amount.compareTo(PricingConstants.PUSH_PER_TIME) == 0;

        if (!isValid) {
            log.warn("Invalid push amount. Expected: {}, Got: {}", PricingConstants.PUSH_PER_TIME, amount);
        }

        return isValid;
    }

    /**
     * Validate membership payment amount
     */
    public boolean validateMembershipAmount(BigDecimal amount, BigDecimal expectedAmount) {
        boolean isValid = amount.compareTo(expectedAmount) == 0;
        
        if (!isValid) {
            log.warn("Invalid membership amount. Expected: {}, Got: {}", expectedAmount, amount);
        }
        
        return isValid;
    }

    /**
     * Validate any payment amount is positive
     */
    public boolean validatePositiveAmount(BigDecimal amount) {
        boolean isValid = amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
        
        if (!isValid) {
            log.warn("Invalid payment amount. Amount must be positive: {}", amount);
        }
        
        return isValid;
    }
}

