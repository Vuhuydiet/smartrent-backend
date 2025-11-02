package com.smartrent.constants;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pricing constants for SmartRent platform
 * All prices are in VND (Vietnamese Dong)
 *
 * Based on business requirements from charge-features-biz.md
 * Updated with new VIP tier system: NORMAL, SILVER, GOLD, DIAMOND
 */
public final class PricingConstants {

    private PricingConstants() {
        // Prevent instantiation
    }

    // =====================================================
    // DAILY BASE PRICES (Base rate per day)
    // =====================================================

    /**
     * Normal listing base price: 2,700 VND/day
     */
    public static final BigDecimal NORMAL_POST_PER_DAY = new BigDecimal("2700");

    /**
     * VIP Silver listing base price: 50,000 VND/day
     */
    public static final BigDecimal SILVER_POST_PER_DAY = new BigDecimal("50000");

    /**
     * VIP Gold listing base price: 110,000 VND/day
     */
    public static final BigDecimal GOLD_POST_PER_DAY = new BigDecimal("110000");

    /**
     * VIP Diamond listing base price: 280,000 VND/day
     */
    public static final BigDecimal DIAMOND_POST_PER_DAY = new BigDecimal("280000");

    // =====================================================
    // DURATION DISCOUNT PERCENTAGES
    // =====================================================

    /**
     * Discount for 10 days: 0% (base price)
     */
    public static final BigDecimal DISCOUNT_10_DAYS = BigDecimal.ZERO;

    /**
     * Discount for 15 days: 11%
     */
    public static final BigDecimal DISCOUNT_15_DAYS = new BigDecimal("0.11");

    /**
     * Discount for 30 days: 18.5%
     */
    public static final BigDecimal DISCOUNT_30_DAYS = new BigDecimal("0.185");

    // =====================================================
    // PUSH PRICES
    // =====================================================

    /**
     * Push price per time: 40,000 VND
     */
    public static final BigDecimal PUSH_PER_TIME = new BigDecimal("40000");

    // =====================================================
    // CURRENCY
    // =====================================================

    /**
     * Default currency for all transactions: VND (Vietnamese Dong)
     */
    public static final String DEFAULT_CURRENCY = "VND";

    // =====================================================
    // STANDARD PRICES (30 days with 18.5% discount)
    // =====================================================

    /**
     * Normal listing price for 30 days: 66,000 VND
     * Calculation: 2,700 × 30 × (1 - 0.185) = 66,015 ≈ 66,000
     */
    public static final BigDecimal NORMAL_POST_30_DAYS = new BigDecimal("66000");

    /**
     * VIP Silver listing price for 30 days: 1,222,500 VND
     * Calculation: 50,000 × 30 × (1 - 0.185) = 1,222,500
     */
    public static final BigDecimal SILVER_POST_30_DAYS = new BigDecimal("1222500");

    /**
     * VIP Gold listing price for 30 days: 2,689,500 VND
     * Calculation: 110,000 × 30 × (1 - 0.185) = 2,689,500
     */
    public static final BigDecimal GOLD_POST_30_DAYS = new BigDecimal("2689500");

    /**
     * VIP Diamond listing price for 30 days: 6,846,000 VND
     * Calculation: 280,000 × 30 × (1 - 0.185) = 6,846,000
     */
    public static final BigDecimal DIAMOND_POST_30_DAYS = new BigDecimal("6846000");

    // =====================================================
    // DURATION CONSTANTS
    // =====================================================

    public static final int DURATION_10_DAYS = 10;
    public static final int DURATION_15_DAYS = 15;
    public static final int DURATION_30_DAYS = 30;

    // =====================================================
    // HELPER METHODS
    // =====================================================

    /**
     * Calculate price with duration-based discount
     * @param basePrice Base price per day
     * @param days Number of days (10, 15, or 30)
     * @return Total price with discount applied
     */
    public static BigDecimal calculatePriceWithDiscount(BigDecimal basePrice, int days) {
        BigDecimal totalBeforeDiscount = basePrice.multiply(new BigDecimal(days));
        BigDecimal discount = getDiscountForDuration(days);
        BigDecimal discountAmount = totalBeforeDiscount.multiply(discount);
        return totalBeforeDiscount.subtract(discountAmount).setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Get discount percentage for duration
     * @param days Number of days
     * @return Discount as decimal (e.g., 0.11 for 11%)
     */
    private static BigDecimal getDiscountForDuration(int days) {
        if (days == DURATION_30_DAYS) {
            return DISCOUNT_30_DAYS;
        } else if (days == DURATION_15_DAYS) {
            return DISCOUNT_15_DAYS;
        } else {
            return DISCOUNT_10_DAYS; // No discount for 10 days or other durations
        }
    }

    /**
     * Calculate price for normal listing based on duration
     * @param days Number of days
     * @return Total price with discount
     */
    public static BigDecimal calculateNormalPostPrice(int days) {
        return calculatePriceWithDiscount(NORMAL_POST_PER_DAY, days);
    }

    /**
     * Calculate price for Silver listing based on duration
     * @param days Number of days
     * @return Total price with discount
     */
    public static BigDecimal calculateSilverPostPrice(int days) {
        return calculatePriceWithDiscount(SILVER_POST_PER_DAY, days);
    }

    /**
     * Calculate price for Gold listing based on duration
     * @param days Number of days
     * @return Total price with discount
     */
    public static BigDecimal calculateGoldPostPrice(int days) {
        return calculatePriceWithDiscount(GOLD_POST_PER_DAY, days);
    }

    /**
     * Calculate price for Diamond listing based on duration
     * @param days Number of days
     * @return Total price with discount
     */
    public static BigDecimal calculateDiamondPostPrice(int days) {
        return calculatePriceWithDiscount(DIAMOND_POST_PER_DAY, days);
    }

    /**
     * Calculate price for multiple pushes
     * @param count Number of pushes
     * @return Total price
     */
    public static BigDecimal calculatePushPrice(int count) {
        return PUSH_PER_TIME.multiply(new BigDecimal(count));
    }

    // =====================================================
    // LISTING LIMITS
    // =====================================================

    /**
     * Maximum images for NORMAL listings
     */
    public static final int NORMAL_MAX_IMAGES = 5;

    /**
     * Maximum videos for NORMAL listings
     */
    public static final int NORMAL_MAX_VIDEOS = 1;

    /**
     * Maximum images for SILVER listings
     */
    public static final int SILVER_MAX_IMAGES = 10;

    /**
     * Maximum videos for SILVER listings
     */
    public static final int SILVER_MAX_VIDEOS = 2;

    /**
     * Maximum images for GOLD listings
     */
    public static final int GOLD_MAX_IMAGES = 12;

    /**
     * Maximum videos for GOLD listings
     */
    public static final int GOLD_MAX_VIDEOS = 2;

    /**
     * Maximum images for DIAMOND listings
     */
    public static final int DIAMOND_MAX_IMAGES = 15;

    /**
     * Maximum videos for DIAMOND listings
     */
    public static final int DIAMOND_MAX_VIDEOS = 3;
}

