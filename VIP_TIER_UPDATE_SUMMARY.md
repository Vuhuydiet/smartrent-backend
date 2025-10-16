# VIP Tier System Update - Implementation Summary

## Overview
Updated the SmartRent backend system from the old 3-tier VIP system (NORMAL, VIP, PREMIUM) to the new 4-tier system (NORMAL, SILVER, GOLD, DIAMOND) as specified in `charge-features-biz.md`.

## Changes Made

### 1. **Enum Updates**

#### VipType Enum (Listing.java)
- **Old:** `NORMAL, VIP, PREMIUM`
- **New:** `NORMAL, SILVER, GOLD, DIAMOND`
- **File:** `smart-rent/src/main/java/com/smartrent/infra/repository/entity/Listing.java`

#### BenefitType Enum
- **Old:** `VIP_POSTS, PREMIUM_POSTS, BOOST_QUOTA, AUTO_VERIFY, TRUSTED_BADGE`
- **New:** `POST_SILVER, POST_GOLD, POST_DIAMOND, BOOST, AUTO_APPROVE, BADGE`
- **File:** `smart-rent/src/main/java/com/smartrent/enums/BenefitType.java`

### 2. **Pricing Constants Update**

**File:** `smart-rent/src/main/java/com/smartrent/constants/PricingConstants.java`

#### New Daily Base Prices
```java
NORMAL_POST_PER_DAY = 2,700 VND
SILVER_POST_PER_DAY = 50,000 VND
GOLD_POST_PER_DAY = 110,000 VND
DIAMOND_POST_PER_DAY = 280,000 VND
```

#### Duration-Based Discounts
```java
DISCOUNT_10_DAYS = 0% (no discount)
DISCOUNT_15_DAYS = 11%
DISCOUNT_30_DAYS = 18.5%
```

#### Standard 30-Day Prices (with 18.5% discount)
```java
NORMAL_POST_30_DAYS = 66,000 VND
SILVER_POST_30_DAYS = 1,222,500 VND
GOLD_POST_30_DAYS = 2,689,500 VND
DIAMOND_POST_30_DAYS = 6,846,000 VND
```

#### New Helper Methods
- `calculatePriceWithDiscount(basePrice, days)` - Applies duration-based discount
- `calculateSilverPostPrice(days)`
- `calculateGoldPostPrice(days)`
- `calculateDiamondPostPrice(days)`

#### Media Limits
```java
NORMAL: 5 images, 1 video
SILVER: 10 images, 2 videos
GOLD: 12 images, 2 videos
DIAMOND: 15 images, 3 videos
```

### 3. **Database Migration**

**File:** `smart-rent/src/main/resources/db/migration/V15__Update_vip_tiers_to_new_system.sql`

#### Changes:
1. **Listings Table:** Updated `vip_type` enum from `NORMAL/VIP/PREMIUM` to `NORMAL/SILVER/GOLD/DIAMOND`
   - VIP → SILVER
   - PREMIUM → DIAMOND

2. **Membership Package Benefits Table:** Updated `benefit_type` enum
   - VIP_POSTS → POST_SILVER
   - PREMIUM_POSTS → POST_DIAMOND
   - BOOST_QUOTA → BOOST
   - AUTO_VERIFY → AUTO_APPROVE
   - TRUSTED_BADGE → BADGE

3. **User Membership Benefits Table:** Same enum updates as above

4. **Added POST_GOLD benefits** to STANDARD and ADVANCED packages

5. **Updated benefit quantities** to match new business logic:
   - STANDARD: 10 Silver, 5 Gold, 2 Diamond, 20 Boosts
   - ADVANCED: 15 Silver, 10 Gold, 5 Diamond, 40 Boosts

### 4. **Service Layer Updates**

#### QuotaServiceImpl
**File:** `smart-rent/src/main/java/com/smartrent/service/quota/impl/QuotaServiceImpl.java`

Updated `checkAllQuotas()` method to return:
```java
{
  "silverPosts": {...},
  "goldPosts": {...},
  "diamondPosts": {...},
  "boosts": {...}
}
```

### 5. **Controller Updates**

#### QuotaController
**File:** `smart-rent/src/main/java/com/smartrent/controller/QuotaController.java`

**New Endpoints:**
- `GET /v1/quotas/silver-posts` - Check Silver post quota
- `GET /v1/quotas/gold-posts` - Check Gold post quota
- `GET /v1/quotas/diamond-posts` - Check Diamond post quota
- `GET /v1/quotas/boosts` - Check boost quota (updated)

**Updated:**
- `GET /v1/quotas/check` - Returns all four quota types
- `GET /v1/quotas/check/{benefitType}` - Accepts new benefit type values

#### ListingController
**File:** `smart-rent/src/main/java/com/smartrent/controller/ListingController.java`

Updated `checkPostingQuota()` endpoint to:
- Accept vipType parameter: "SILVER", "GOLD", "DIAMOND"
- Return all four quota types when no parameter specified

### 6. **Validator Updates**

#### QuotaAvailabilityValidator
**File:** `smart-rent/src/main/java/com/smartrent/validator/QuotaAvailabilityValidator.java`

**New Methods:**
- `hasSilverPostQuota(userId)` - Replaces `hasVipPostQuota()`
- `hasGoldPostQuota(userId)` - New method
- `hasDiamondPostQuota(userId)` - Replaces `hasPremiumPostQuota()`
- `hasBoostQuota(userId)` - Updated to use `BenefitType.BOOST`

### 7. **Entity Helper Methods**

#### Listing Entity
**File:** `smart-rent/src/main/java/com/smartrent/infra/repository/entity/Listing.java`

**New Helper Methods:**
- `isSilver()` - Replaces `isVip()`
- `isGold()` - New method
- `isDiamond()` - Replaces `isPremium()`

## Business Logic Alignment

### Pricing Structure (from charge-features-biz.md)

#### NORMAL (TIN THƯỜNG)
- Base: 2,700 VND/day
- 10 days: 27,000 VND
- 15 days: 36,000 VND (11% off)
- 30 days: 66,000 VND (18.5% off)

#### SILVER (VIP BẠC)
- Base: 50,000 VND/day
- 10 days: 500,000 VND
- 15 days: 667,500 VND (11% off)
- 30 days: 1,222,500 VND (18.5% off)

#### GOLD (VIP VÀNG)
- Base: 110,000 VND/day
- 10 days: 1,100,000 VND
- 15 days: 1,468,500 VND (11% off)
- 30 days: 2,689,500 VND (18.5% off)

#### DIAMOND (VIP KIM CƯƠNG)
- Base: 280,000 VND/day
- 10 days: 2,800,000 VND
- 15 days: 3,738,000 VND (11% off)
- 30 days: 6,846,000 VND (18.5% off)

### Membership Package Benefits

#### BASIC Package
- 5 Silver posts
- 10 Boosts

#### STANDARD Package
- 10 Silver posts
- 5 Gold posts
- 2 Diamond posts
- 20 Boosts
- Auto-approve

#### ADVANCED Package
- 15 Silver posts
- 10 Gold posts
- 5 Diamond posts
- 40 Boosts
- Auto-approve
- Trusted badge

## Files Modified

1. `smart-rent/src/main/java/com/smartrent/infra/repository/entity/Listing.java`
2. `smart-rent/src/main/java/com/smartrent/constants/PricingConstants.java`
3. `smart-rent/src/main/java/com/smartrent/enums/BenefitType.java`
4. `smart-rent/src/main/java/com/smartrent/service/quota/impl/QuotaServiceImpl.java`
5. `smart-rent/src/main/java/com/smartrent/controller/QuotaController.java`
6. `smart-rent/src/main/java/com/smartrent/controller/ListingController.java`
7. `smart-rent/src/main/java/com/smartrent/validator/QuotaAvailabilityValidator.java`

## Files Created

1. `smart-rent/src/main/resources/db/migration/V15__Update_vip_tiers_to_new_system.sql`
2. `VIP_TIER_UPDATE_SUMMARY.md` (this file)

## Migration Notes

### Database Migration Steps
1. Run the V15 migration script to update enum values
2. Existing data will be automatically migrated:
   - VIP listings → SILVER
   - PREMIUM listings → DIAMOND
   - VIP_POSTS benefits → POST_SILVER
   - PREMIUM_POSTS benefits → POST_DIAMOND

### API Compatibility
- Old endpoints still exist but use new enum values internally
- Frontend should be updated to use new tier names: SILVER, GOLD, DIAMOND
- Quota check responses now include all four tiers

## Testing Recommendations

1. **Database Migration Testing:**
   - Verify enum values are correctly updated
   - Check existing listings maintain their tier status
   - Confirm user benefits are properly migrated

2. **API Testing:**
   - Test all quota check endpoints
   - Verify pricing calculations with different durations
   - Test listing creation with each VIP tier

3. **Business Logic Testing:**
   - Verify duration-based discounts are applied correctly
   - Test quota consumption for each tier
   - Confirm media limits are enforced per tier

## Next Steps

1. Update frontend to use new VIP tier names
2. Update API documentation/Swagger with new tier information
3. Test the complete flow: membership purchase → quota grant → listing creation
4. Update any remaining services that may reference old enum values
5. Consider adding migration script for production data backup before running V15

## Backward Compatibility

The migration script handles backward compatibility by:
- Automatically converting old enum values to new ones
- Preserving all existing data relationships
- Maintaining quota balances during migration
- No data loss during the transition

