package com.smartrent.service.repost.impl;

import com.smartrent.config.Constants;
import com.smartrent.constants.PricingConstants;
import com.smartrent.dto.request.PaymentRequest;
import com.smartrent.dto.request.RepostListingRequest;
import com.smartrent.dto.response.PaymentResponse;
import com.smartrent.dto.response.RepostResponse;
import com.smartrent.enums.BenefitType;
import com.smartrent.enums.ListingStatus;
import com.smartrent.enums.PaymentProvider;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.TransactionRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.Transaction;
import com.smartrent.service.payment.PaymentService;
import com.smartrent.service.quota.QuotaService;
import com.smartrent.service.repost.RepostService;
import com.smartrent.service.transaction.TransactionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mirrors {@link com.smartrent.service.push.impl.PushServiceImpl} but for
 * "đăng lại" (re-publish an expired listing). The user either consumes the
 * post-quota matching their listing's vipType or pays the per-day listing
 * fee via VNPay; on success the listing's expired flag is cleared, a fresh
 * expiryDate / postDate is set, and pushedAt is refreshed so it reappears
 * at the top of search.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RepostServiceImpl implements RepostService {

    ListingRepository listingRepository;
    TransactionRepository transactionRepository;
    QuotaService quotaService;
    TransactionService transactionService;
    PaymentService paymentService;

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = Constants.CacheNames.LISTING_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = Constants.CacheNames.LISTING_BROWSE, allEntries = true),
            @CacheEvict(cacheNames = Constants.CacheNames.LISTING_DETAIL, allEntries = true)
    })
    public RepostResponse repostListing(String userId, RepostListingRequest request) {
        log.info("Reposting listing {} for user {}", request.getListingId(), userId);

        Listing listing = listingRepository.findById(request.getListingId())
                .orElseThrow(() -> new RuntimeException("Listing not found: " + request.getListingId()));

        if (!listing.getUserId().equals(userId)) {
            throw new RuntimeException("Listing does not belong to user");
        }

        validateListingCanBeReposted(listing);

        int durationDays = resolveDurationDays(request.getDurationDays(), listing.getDurationDays());
        BigDecimal feeAmount = calculateRepostFee(listing.getVipType(), durationDays);
        BenefitType benefitType = mapVipTypeToBenefitType(listing.getVipType());

        boolean useQuota = Boolean.TRUE.equals(request.getUseMembershipQuota());

        if (useQuota && benefitType != null) {
            var quotaStatus = quotaService.checkQuotaAvailability(userId, benefitType);
            log.info("Quota status for user {} ({}): available={}, used={}, granted={}",
                    userId, benefitType, quotaStatus.getTotalAvailable(),
                    quotaStatus.getTotalUsed(), quotaStatus.getTotalGranted());

            if (quotaStatus.getTotalAvailable() > 0) {
                boolean consumed = quotaService.consumeQuota(userId, benefitType, 1);
                if (!consumed) {
                    log.error("Failed to consume {} quota for user {}", benefitType, userId);
                    throw new RuntimeException("Failed to consume repost quota");
                }
                return reactivateListing(listing, durationDays, "MEMBERSHIP_QUOTA",
                        "Listing reposted successfully using quota");
            }
            log.info("User {} has no available {} quota — falling back to payment",
                    userId, benefitType);
        }

        // Direct payment path — listing stays expired until callback completes
        String transactionId = transactionService.createRepostFeeTransaction(
                userId,
                request.getListingId(),
                feeAmount,
                durationDays,
                request.getPaymentProvider() != null ? request.getPaymentProvider() : "VNPAY"
        );

        PaymentRequest paymentRequest = PaymentRequest.builder()
                .transactionId(transactionId)
                .provider(PaymentProvider.valueOf(
                        request.getPaymentProvider() != null ? request.getPaymentProvider() : "VNPAY"))
                .amount(feeAmount)
                .currency(PricingConstants.DEFAULT_CURRENCY)
                .orderInfo("SmartRent Repost Listing " + request.getListingId())
                .build();

        PaymentResponse paymentResponse = paymentService.createPayment(paymentRequest, null);
        log.info("Payment URL generated for repost transaction: {}", transactionId);

        return RepostResponse.builder()
                .listingId(request.getListingId())
                .userId(userId)
                .repostSource("PAYMENT_REQUIRED")
                .durationDays(durationDays)
                .message("Payment required")
                .paymentUrl(paymentResponse.getPaymentUrl())
                .transactionId(paymentResponse.getTransactionRef())
                .build();
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = Constants.CacheNames.LISTING_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = Constants.CacheNames.LISTING_BROWSE, allEntries = true),
            @CacheEvict(cacheNames = Constants.CacheNames.LISTING_DETAIL, allEntries = true)
    })
    public RepostResponse completeRepostAfterPayment(String transactionId) {
        log.info("Completing repost after payment for transaction: {}", transactionId);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        if (!transaction.isCompleted()) {
            throw new RuntimeException("Transaction is not completed: " + transactionId);
        }
        if (!transaction.isRepostFee()) {
            throw new RuntimeException("Transaction is not a repost fee: " + transactionId);
        }

        Long listingId = Long.parseLong(transaction.getReferenceId());
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found: " + listingId));

        // Recover durationDays from the additionalInfo we stored at creation time.
        int durationDays = parseDurationDaysFromInfo(transaction.getAdditionalInfo(),
                listing.getDurationDays());

        return reactivateListing(listing, durationDays, "DIRECT_PAYMENT",
                "Listing reposted successfully after payment");
    }

    // ============================================================
    //  Helpers
    // ============================================================

    private RepostResponse reactivateListing(
            Listing listing, int durationDays, String source, String message) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime newExpiry = now.plusDays(durationDays);

        listing.setExpired(false);
        listing.setExpiryDate(newExpiry);
        listing.setPostDate(now);
        listing.setPushedAt(now);
        listing.setDurationDays(durationDays);
        listingRepository.save(listing);

        log.info("Successfully reactivated listing {} via {} — new expiry {}",
                listing.getListingId(), source, newExpiry);

        return RepostResponse.builder()
                .listingId(listing.getListingId())
                .userId(listing.getUserId())
                .repostSource(source)
                .repostedAt(now)
                .expiryDate(newExpiry)
                .durationDays(durationDays)
                .message(message)
                .build();
    }

    private void validateListingCanBeReposted(Listing listing) {
        ListingStatus status = listing.computeListingStatus();
        if (status != ListingStatus.EXPIRED) {
            throw new RuntimeException(
                    "Only expired listings can be reposted. Current status: " + status);
        }
    }

    /**
     * Pick a duration in days. Only 10 / 15 / 30 are valid (those are the
     * tiers with documented pricing); anything else falls back to the
     * listing's existing duration, then to 30.
     */
    private int resolveDurationDays(Integer requested, Integer existing) {
        if (requested != null && isValidDuration(requested)) {
            return requested;
        }
        if (existing != null && isValidDuration(existing)) {
            return existing;
        }
        return PricingConstants.DURATION_30_DAYS;
    }

    private boolean isValidDuration(int days) {
        return days == PricingConstants.DURATION_10_DAYS
                || days == PricingConstants.DURATION_15_DAYS
                || days == PricingConstants.DURATION_30_DAYS;
    }

    private BigDecimal calculateRepostFee(Listing.VipType vipType, int durationDays) {
        return switch (vipType) {
            case NORMAL -> PricingConstants.calculateNormalPostPrice(durationDays);
            case SILVER -> PricingConstants.calculateSilverPostPrice(durationDays);
            case GOLD -> PricingConstants.calculateGoldPostPrice(durationDays);
            case DIAMOND -> PricingConstants.calculateDiamondPostPrice(durationDays);
        };
    }

    /**
     * NORMAL listings have no free post quota — they always go through
     * the payment path. SILVER / GOLD / DIAMOND each map to their
     * dedicated benefit type.
     */
    private BenefitType mapVipTypeToBenefitType(Listing.VipType vipType) {
        return switch (vipType) {
            case NORMAL -> null;
            case SILVER -> BenefitType.POST_SILVER;
            case GOLD -> BenefitType.POST_GOLD;
            case DIAMOND -> BenefitType.POST_DIAMOND;
        };
    }

    /**
     * Best-effort recovery of durationDays from the transaction's
     * additionalInfo string ("Pay-per-repost fee for {N} days"). If parsing
     * fails we fall back to the listing's stored durationDays so the user
     * still gets a sensible new expiry rather than a hard error.
     */
    private int parseDurationDaysFromInfo(String info, Integer fallback) {
        if (info != null) {
            int forIdx = info.indexOf(" for ");
            int daysIdx = info.indexOf(" days");
            if (forIdx > 0 && daysIdx > forIdx) {
                try {
                    return Integer.parseInt(info.substring(forIdx + 5, daysIdx).trim());
                } catch (NumberFormatException ignored) {
                    // fall through
                }
            }
        }
        if (fallback != null && isValidDuration(fallback)) {
            return fallback;
        }
        return PricingConstants.DURATION_30_DAYS;
    }
}
