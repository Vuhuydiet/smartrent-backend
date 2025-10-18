package com.smartrent.service.boost.impl;

import com.smartrent.constants.PricingConstants;
import com.smartrent.dto.request.BoostListingRequest;
import com.smartrent.dto.request.PaymentRequest;
import com.smartrent.dto.request.ScheduleBoostRequest;
import com.smartrent.dto.response.BoostResponse;
import com.smartrent.dto.response.PaymentResponse;
import com.smartrent.enums.*;
import com.smartrent.infra.repository.*;
import com.smartrent.infra.repository.entity.*;
import com.smartrent.service.boost.BoostService;
import com.smartrent.service.quota.QuotaService;
import com.smartrent.service.payment.PaymentService;
import com.smartrent.service.transaction.TransactionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BoostServiceImpl implements BoostService {

    ListingRepository listingRepository;
    PushHistoryRepository pushHistoryRepository;
    PushScheduleRepository pushScheduleRepository;
    UserMembershipBenefitRepository userBenefitRepository;
    TransactionRepository transactionRepository;
    QuotaService quotaService;
    TransactionService transactionService;
    PaymentService paymentService;

    @Override
    @Transactional
    public BoostResponse boostListing(String userId, BoostListingRequest request) {
        log.info("Boosting listing {} for user {}", request.getListingId(), userId);

        // Validate listing exists and belongs to user
        Listing listing = listingRepository.findById(request.getListingId())
                .orElseThrow(() -> new RuntimeException("Listing not found: " + request.getListingId()));

        if (!listing.getUserId().equals(userId)) {
            throw new RuntimeException("Listing does not belong to user");
        }

        // Check if user wants to use quota
        boolean useQuota = Boolean.TRUE.equals(request.getUseMembershipQuota());

        if (useQuota) {
            // Check quota availability
            var quotaStatus = quotaService.checkQuotaAvailability(userId, BenefitType.BOOST);

            if (quotaStatus.getTotalAvailable() > 0) {
                // Use membership quota
                boolean consumed = quotaService.consumeQuota(userId, BenefitType.BOOST, 1);
                if (!consumed) {
                    throw new RuntimeException("Failed to consume boost quota");
                }

                // Create push history with MEMBERSHIP_QUOTA source
                PushHistory pushHistory = PushHistory.builder()
                        .listingId(listing.getListingId())
                        .userId(userId)
                        .pushSource(PushSource.MEMBERSHIP_QUOTA)
                        .userBenefit(null) // Can be set if we track which benefit was used
                        .transactionId(null)
                        .pushedAt(LocalDateTime.now())
                        .build();

                // Update listing's pushed_at timestamp
                listing.setPushedAt(LocalDateTime.now());
                listing.setPostDate(LocalDateTime.now()); // Boost = update post_date to push to top
                listingRepository.save(listing);

                // Save push history
                pushHistory = pushHistoryRepository.save(pushHistory);

                // If listing is Gold or Diamond, also boost its shadow listing
                if ((listing.isGold() || listing.isDiamond()) && !listing.isShadowListing()) {
                    boostShadowListing(listing, userId);
                }

                log.info("Successfully boosted listing {} using quota", request.getListingId());
                return mapToBoostResponse(pushHistory, "Listing boosted successfully using quota");
            }
        }

        // No quota or user chose not to use quota - require payment
        log.info("No quota available or user chose payment - initiating payment flow for boost");

        // Create PENDING transaction for boost
        String transactionId = transactionService.createBoostFeeTransaction(
                userId,
                request.getListingId(),
                PricingConstants.BOOST_PER_TIME,
                request.getPaymentProvider() != null ? request.getPaymentProvider() : "VNPAY"
        );

        // Generate payment URL
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .provider(com.smartrent.enums.PaymentProvider.valueOf(
                        request.getPaymentProvider() != null ? request.getPaymentProvider() : "VNPAY"))
                .amount(PricingConstants.BOOST_PER_TIME)
                .orderInfo("Boost listing #" + request.getListingId())
                .returnUrl(request.getReturnUrl())
                .build();

        PaymentResponse paymentResponse = paymentService.createPayment(paymentRequest, null);

        log.info("Payment URL generated for boost transaction: {}", transactionId);

        // Return payment response wrapped in BoostResponse
        return BoostResponse.builder()
                .listingId(request.getListingId())
                .userId(userId)
                .pushSource("PAYMENT_REQUIRED")
                .message("Payment required")
                .paymentUrl(paymentResponse.getPaymentUrl())
                .transactionId(paymentResponse.getTransactionRef())
                .build();
    }

    /**
     * Complete boost after successful payment
     * Called from payment callback handler
     */
    @Transactional
    public BoostResponse completeBoostAfterPayment(String transactionId) {
        log.info("Completing boost after payment for transaction: {}", transactionId);

        // Get transaction
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        if (!transaction.isCompleted()) {
            throw new RuntimeException("Transaction is not completed: " + transactionId);
        }

        if (!transaction.isBoostFee()) {
            throw new RuntimeException("Transaction is not a boost fee: " + transactionId);
        }

        // Get listing ID from transaction reference
        Long listingId = Long.parseLong(transaction.getReferenceId());
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found: " + listingId));

        // Create push history with DIRECT_PAYMENT source
        PushHistory pushHistory = PushHistory.builder()
                .listingId(listing.getListingId())
                .userId(transaction.getUserId())
                .pushSource(PushSource.DIRECT_PAYMENT)
                .userBenefit(null)
                .transactionId(transactionId)
                .pushedAt(LocalDateTime.now())
                .build();

        // Update listing's pushed_at timestamp
        listing.setPushedAt(LocalDateTime.now());
        listing.setPostDate(LocalDateTime.now());
        listingRepository.save(listing);

        // Save push history
        pushHistory = pushHistoryRepository.save(pushHistory);

        // If listing is Gold or Diamond, also boost its shadow listing
        if ((listing.isGold() || listing.isDiamond()) && !listing.isShadowListing()) {
            boostShadowListing(listing, transaction.getUserId());
        }

        log.info("Successfully boosted listing {} after payment", listingId);
        return mapToBoostResponse(pushHistory, "Listing boosted successfully after payment");
    }

    @Override
    @Transactional
    public BoostResponse scheduleBoost(String userId, ScheduleBoostRequest request) {
        log.info("Scheduling boost for listing {} at time {}", request.getListingId(), request.getScheduledTime());

        // Validate listing
        Listing listing = listingRepository.findById(request.getListingId())
                .orElseThrow(() -> new RuntimeException("Listing not found: " + request.getListingId()));

        if (!listing.getUserId().equals(userId)) {
            throw new RuntimeException("Listing does not belong to user");
        }

        Transaction transaction = null;
        ScheduleSource source;
        Long sourceId = null;

        if (Boolean.TRUE.equals(request.getUseMembershipQuota())) {
            // Check if user has enough quota
            Integer availableQuota = userBenefitRepository.getTotalAvailableQuota(
                    userId, BenefitType.BOOST, LocalDateTime.now());

            if (availableQuota == null || availableQuota < request.getTotalPushes()) {
                throw new RuntimeException("Insufficient boost quota. Available: " + availableQuota +
                        ", Required: " + request.getTotalPushes());
            }

            source = ScheduleSource.MEMBERSHIP;
        } else {
            // Direct purchase
            transaction = createScheduledBoostTransaction(userId, request.getListingId(), 
                    request.getTotalPushes(), request.getPaymentProvider());
            transactionRepository.save(transaction);
            transaction.complete();
            transactionRepository.save(transaction);

            source = ScheduleSource.DIRECT_PURCHASE;
        }

        // Create schedule
        PushSchedule schedule = PushSchedule.builder()
                .userId(userId)
                .listingId(request.getListingId())
                .scheduledTime(request.getScheduledTime())
                .source(source)
                .sourceId(sourceId)
                .totalPushes(request.getTotalPushes())
                .usedPushes(0)
                .status(ScheduleStatus.ACTIVE)
                .transaction(transaction)
                .build();

        schedule = pushScheduleRepository.save(schedule);

        log.info("Successfully scheduled boost for listing {}", request.getListingId());
        return BoostResponse.builder()
                .listingId(request.getListingId())
                .userId(userId)
                .pushSource(source.name())
                .message("Boost scheduled successfully for " + request.getScheduledTime())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoostResponse> getBoostHistory(Long listingId) {
        log.info("Getting boost history for listing: {}", listingId);
        return pushHistoryRepository.findByListingIdOrderByPushedAtDesc(listingId)
                .stream()
                .map(ph -> mapToBoostResponse(ph, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoostResponse> getUserBoostHistory(String userId) {
        log.info("Getting boost history for user: {}", userId);
        return pushHistoryRepository.findByUserIdOrderByPushedAtDesc(userId)
                .stream()
                .map(ph -> mapToBoostResponse(ph, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public int executeScheduledBoosts() {
        log.info("Executing scheduled boosts");
        LocalTime currentTime = LocalTime.now().withSecond(0).withNano(0);
        
        List<PushSchedule> schedules = pushScheduleRepository.findActiveSchedulesByTime(currentTime);
        int executedCount = 0;

        for (PushSchedule schedule : schedules) {
            try {
                executeScheduledBoost(schedule);
                executedCount++;
            } catch (Exception e) {
                log.error("Failed to execute scheduled boost for schedule {}: {}", 
                        schedule.getScheduleId(), e.getMessage());
            }
        }

        log.info("Executed {} scheduled boosts", executedCount);
        return executedCount;
    }

    @Override
    @Transactional
    public void cancelScheduledBoost(String userId, Long scheduleId) {
        log.info("Cancelling scheduled boost {} for user {}", scheduleId, userId);
        
        PushSchedule schedule = pushScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found: " + scheduleId));

        if (!schedule.getUserId().equals(userId)) {
            throw new RuntimeException("Schedule does not belong to user");
        }

        schedule.cancel();
        pushScheduleRepository.save(schedule);

        log.info("Successfully cancelled scheduled boost {}", scheduleId);
    }

    // Helper methods
    private void executeScheduledBoost(PushSchedule schedule) {
        if (!schedule.hasRemainingPushes()) {
            log.warn("Schedule {} has no remaining pushes", schedule.getScheduleId());
            return;
        }

        Listing listing = listingRepository.findById(schedule.getListingId())
                .orElseThrow(() -> new RuntimeException("Listing not found: " + schedule.getListingId()));

        UserMembershipBenefit usedBenefit = null;

        // Consume quota if from membership
        if (schedule.isFromMembership()) {
            UserMembershipBenefit benefit = userBenefitRepository
                    .findFirstAvailableBenefit(schedule.getUserId(), BenefitType.BOOST, LocalDateTime.now())
                    .orElse(null);

            if (benefit != null) {
                benefit.consumeQuota(1);
                userBenefitRepository.save(benefit);
                usedBenefit = benefit;
            } else {
                log.warn("No quota available for scheduled boost, skipping");
                return;
            }
        }

        // Create push history
        PushHistory pushHistory = createPushHistory(
                listing.getListingId(), 
                schedule.getUserId(), 
                PushSource.SCHEDULED, 
                usedBenefit, 
                schedule
        );
        pushHistoryRepository.save(pushHistory);

        // Update listing
        listing.setPushedAt(LocalDateTime.now());
        listing.setPostDate(LocalDateTime.now());
        listingRepository.save(listing);

        // Boost shadow listing if Gold or Diamond
        if ((listing.isGold() || listing.isDiamond()) && !listing.isShadowListing()) {
            boostShadowListing(listing, schedule.getUserId());
        }

        // Update schedule
        schedule.incrementUsedPushes();
        pushScheduleRepository.save(schedule);

        log.info("Executed scheduled boost for listing {}", listing.getListingId());
    }

    private void boostShadowListing(Listing premiumListing, String userId) {
        // Find shadow listing
        List<Listing> shadowListings = listingRepository.findAll().stream()
                .filter(l -> l.getParentListingId() != null && 
                           l.getParentListingId().equals(premiumListing.getListingId()))
                .collect(Collectors.toList());

        for (Listing shadow : shadowListings) {
            shadow.setPushedAt(LocalDateTime.now());
            shadow.setPostDate(LocalDateTime.now());
            listingRepository.save(shadow);

            PushHistory shadowPush = createPushHistory(
                    shadow.getListingId(), 
                    userId, 
                    PushSource.ADMIN, // Free boost from Premium
                    null, 
                    null
            );
            pushHistoryRepository.save(shadowPush);

            log.info("Boosted shadow listing {} for Premium listing {}", 
                    shadow.getListingId(), premiumListing.getListingId());
        }
    }

    private PushHistory createPushHistory(Long listingId, String userId, PushSource source, 
                                         UserMembershipBenefit benefit, PushSchedule schedule) {
        return PushHistory.builder()
                .listingId(listingId)
                .userId(userId)
                .pushSource(source)
                .userBenefit(benefit)
                .schedule(schedule)
                .build();
    }

    private Transaction createBoostTransaction(String userId, Long listingId, String paymentProvider) {
        return Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .userId(userId)
                .transactionType(TransactionType.BOOST_FEE)
                .amount(java.math.BigDecimal.valueOf(50000)) // 50,000 VND per boost
                .referenceType(ReferenceType.BOOST)
                .referenceId(listingId.toString())
                .status(TransactionStatus.PENDING)
                .paymentProvider(paymentProvider != null ? PaymentProvider.valueOf(paymentProvider) : PaymentProvider.VNPAY)
                .build();
    }

    private Transaction createScheduledBoostTransaction(String userId, Long listingId, int totalPushes, String paymentProvider) {
        long amount = 50000L * totalPushes; // 50,000 VND per boost
        return Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .userId(userId)
                .transactionType(TransactionType.BOOST_FEE)
                .amount(java.math.BigDecimal.valueOf(amount))
                .referenceType(ReferenceType.BOOST)
                .referenceId(listingId.toString())
                .additionalInfo("Scheduled boost package: " + totalPushes + " pushes")
                .status(TransactionStatus.PENDING)
                .paymentProvider(paymentProvider != null ? PaymentProvider.valueOf(paymentProvider) : PaymentProvider.VNPAY)
                .build();
    }

    private BoostResponse mapToBoostResponse(PushHistory pushHistory, String message) {
        return BoostResponse.builder()
                .pushId(pushHistory.getPushId())
                .listingId(pushHistory.getListingId())
                .userId(pushHistory.getUserId())
                .pushSource(pushHistory.getPushSource().name())
                .pushedAt(pushHistory.getPushedAt())
                .message(message)
                .build();
    }
}

