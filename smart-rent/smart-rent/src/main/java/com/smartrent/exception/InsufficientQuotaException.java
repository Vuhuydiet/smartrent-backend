package com.smartrent.exception;

import com.smartrent.enums.BenefitType;

/**
 * Exception thrown when user has insufficient quota for an action
 */
public class InsufficientQuotaException extends RuntimeException {

    private final String userId;
    private final BenefitType benefitType;
    private final int required;
    private final int available;

    public InsufficientQuotaException(String userId, BenefitType benefitType, int required, int available) {
        super(String.format("Insufficient %s quota for user %s. Required: %d, Available: %d", 
                benefitType, userId, required, available));
        this.userId = userId;
        this.benefitType = benefitType;
        this.required = required;
        this.available = available;
    }

    public InsufficientQuotaException(String userId, BenefitType benefitType) {
        super(String.format("No %s quota available for user %s", benefitType, userId));
        this.userId = userId;
        this.benefitType = benefitType;
        this.required = 1;
        this.available = 0;
    }

    public String getUserId() {
        return userId;
    }

    public BenefitType getBenefitType() {
        return benefitType;
    }

    public int getRequired() {
        return required;
    }

    public int getAvailable() {
        return available;
    }
}

