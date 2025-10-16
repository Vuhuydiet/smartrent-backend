package com.smartrent.enums;

/**
 * Enum representing different types of membership benefits
 * These are hardcoded as each benefit requires specific business logic
 * Updated to match new VIP tier system: SILVER, GOLD, DIAMOND
 */
public enum BenefitType {
    /**
     * Free VIP Silver post quota
     */
    POST_SILVER,

    /**
     * Free VIP Gold post quota
     */
    POST_GOLD,

    /**
     * Free VIP Diamond post quota
     */
    POST_DIAMOND,

    /**
     * Free boost/push quota
     */
    BOOST,

    /**
     * Auto-verify listings immediately
     */
    AUTO_APPROVE,

    /**
     * Trusted partner badge
     */
    BADGE
}

