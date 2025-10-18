package com.smartrent.service.push.impl;

import com.smartrent.constants.PricingConstants;
import com.smartrent.dto.request.BoostListingRequest;
import com.smartrent.dto.request.PaymentRequest;
import com.smartrent.dto.request.ScheduleBoostRequest;
import com.smartrent.dto.response.BoostResponse;
import com.smartrent.dto.response.PaymentResponse;
import com.smartrent.enums.*;
import com.smartrent.infra.repository.*;
import com.smartrent.infra.repository.entity.*;
import com.smartrent.service.push.PushService;
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
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of PushService.
 * Handles listing push/boost operations with proper transaction management and error handling.
 * Consolidated service for both immediate boosts and scheduled pushes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PushServiceImpl implements PushService {

    ListingRepository listingRepository;
    PushScheduleRepository pushScheduleRepository;
    PushHistoryRepository pushHistoryRepository;
    UserMembershipBenefitRepository userBenefitRepository;
    TransactionRepository transactionRepository;
    QuotaService quotaService;
    TransactionService transactionService;
    PaymentService paymentService;

    // ========== Boost/Push Listing Methods ==========

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
                        .pushSource(PushHistory.PushSource.MEMBERSHIP_QUOTA)
                        .status(PushHistory.PushStatus.SUCCESS)
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
                .provider(PaymentProvider.valueOf(
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

    @Override
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
                .pushSource(PushHistory.PushSource.DIRECT_PAYMENT)
                .transactionId(transactionId)
                .status(PushHistory.PushStatus.SUCCESS)
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
        PushSchedule.PushSource source;
        Long sourceId = null;

        if (Boolean.TRUE.equals(request.getUseMembershipQuota())) {
            // Check if user has enough quota
            Integer availableQuota = userBenefitRepository.getTotalAvailableQuota(
                    userId, BenefitType.BOOST, LocalDateTime.now());

            if (availableQuota == null || availableQuota < request.getTotalPushes()) {
                throw new RuntimeException("Insufficient boost quota. Available: " + availableQuota +
                        ", Required: " + request.getTotalPushes());
            }

            source = PushSchedule.PushSource.MEMBERSHIP;
        } else {
            // Direct purchase - create transaction
            String txnId = transactionService.createBoostFeeTransaction(
                    userId,
                    request.getListingId(),
                    PricingConstants.BOOST_PER_TIME.multiply(java.math.BigDecimal.valueOf(request.getTotalPushes())),
                    request.getPaymentProvider() != null ? request.getPaymentProvider() : "VNPAY"
            );

            transaction = transactionRepository.findById(txnId).orElse(null);
            source = PushSchedule.PushSource.DIRECT_PURCHASE;
        }        // Create schedule
        PushSchedule schedule = PushSchedule.builder()
                .userId(userId)
                .listingId(request.getListingId())
                .scheduledTime(request.getScheduledTime())
                .source(source)
                .sourceId(sourceId)
                .totalPushes(request.getTotalPushes())
                .usedPushes(0)
                .status(PushSchedule.ScheduleStatus.ACTIVE)
                .transactionId(transaction != null ? transaction.getTransactionId() : null)
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

        // Find all listings owned by the user
        List<Listing> userListings = listingRepository.findByUserId(userId);
        List<Long> listingIds = userListings.stream()
                .map(Listing::getListingId)
                .collect(Collectors.toList());

        if (listingIds.isEmpty()) {
            log.info("No listings found for user: {}", userId);
            return List.of();
        }

        // Find all push history for these listings
        return pushHistoryRepository.findByListingIdInOrderByPushedAtDesc(listingIds)
                .stream()
                .map(ph -> mapToBoostResponse(ph, null))
                .collect(Collectors.toList());
    }

    // ========== Legacy Push Schedule Methods ==========

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public PushSchedule createSchedule(Long listingId, LocalTime scheduledTime, LocalDateTime endTime) {
        log.info("Creating push schedule: listingId={}, scheduledTime={}, endTime={}",
                listingId, scheduledTime, endTime);

        // Validate listing exists
        if (!listingRepository.existsById(listingId)) {
            log.error("Cannot create schedule: Listing not found: listingId={}", listingId);
            throw new IllegalArgumentException("Listing not found with ID: " + listingId);
        }

        // Check if listing already has an active schedule
        boolean hasActiveSchedule = pushScheduleRepository
                .existsByListingIdAndStatus(listingId, PushSchedule.ScheduleStatus.ACTIVE);

        if (hasActiveSchedule) {
            log.error("Cannot create schedule: Listing already has an active schedule: listingId={}", listingId);
            throw new IllegalStateException("Listing already has an active schedule. Deactivate or delete the existing schedule first.");
        }

        // Validate end time is in the future
        if (endTime.isBefore(LocalDateTime.now())) {
            log.error("Cannot create schedule: End time is in the past: endTime={}", endTime);
            throw new IllegalArgumentException("End time must be in the future");
        }

        // Create the schedule
        PushSchedule schedule = PushSchedule.builder()
                .listingId(listingId)
                .scheduledTime(scheduledTime)
                .status(PushSchedule.ScheduleStatus.ACTIVE)
                .build();

        PushSchedule savedSchedule = pushScheduleRepository.save(schedule);
        log.info("Successfully created push schedule: scheduleId={}, listingId={}",
                savedSchedule.getScheduleId(), listingId);

        return savedSchedule;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public boolean pushListing(Long scheduleId, Long listingId, LocalDateTime pushTime) {
        log.info("Attempting to push listing: listingId={}, scheduleId={}, pushTime={}",
                listingId, scheduleId, pushTime);

        try {
            // Verify listing exists
            Optional<Listing> listingOpt = listingRepository.findById(listingId);
            if (listingOpt.isEmpty()) {
                log.warn("Listing not found: listingId={}", listingId);
                createPushHistory(scheduleId, listingId, PushHistory.PushStatus.FAIL,
                        "Listing not found", pushTime);
                return false;
            }

            Listing listing = listingOpt.get();

            // Update pushed_at timestamp
            listing.setPushedAt(pushTime);
            listingRepository.save(listing);

            // Create success history record
            createPushHistory(scheduleId, listingId, PushHistory.PushStatus.SUCCESS,
                    "Successfully pushed listing", pushTime);

            log.info("Successfully pushed listing: listingId={}, scheduleId={}", listingId, scheduleId);
            return true;

        } catch (Exception e) {
            log.error("Failed to push listing: listingId={}, scheduleId={}, error={}",
                    listingId, scheduleId, e.getMessage(), e);

            // Create failure history record
            createPushHistory(scheduleId, listingId, PushHistory.PushStatus.FAIL,
                    "Error: " + e.getMessage(), pushTime);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int processScheduledPushes(LocalDateTime currentTime) {
        log.info("Processing scheduled pushes for time: {}", currentTime);

        // Extract the hour component (e.g., 09:00:00 from 2025-10-16 09:30:45)
        LocalTime scheduledTime = LocalTime.of(currentTime.getHour(), 0, 0);

        // Find all active schedules for this hour
        List<PushSchedule> schedules = pushScheduleRepository
                .findActiveSchedulesByScheduledTime(scheduledTime, currentTime);

        log.info("Found {} active schedules for time: {}", schedules.size(), scheduledTime);

        int successCount = 0;
        for (PushSchedule schedule : schedules) {
            try {
                boolean success = pushListing(
                        schedule.getScheduleId(),
                        schedule.getListingId(),
                        currentTime
                );
                if (success) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("Error processing schedule: scheduleId={}, listingId={}, error={}",
                        schedule.getScheduleId(), schedule.getListingId(), e.getMessage(), e);
            }
        }

        log.info("Completed processing scheduled pushes. Success: {}/{}", successCount, schedules.size());
        return successCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int expireOldSchedules(LocalDateTime currentTime) {
        log.info("Checking for expired schedules at: {}", currentTime);

        List<PushSchedule> expiredSchedules = pushScheduleRepository.findExpiredSchedules(currentTime);
        log.info("Found {} expired schedules", expiredSchedules.size());

        int expiredCount = 0;
        for (PushSchedule schedule : expiredSchedules) {
            try {
                schedule.setStatus(PushSchedule.ScheduleStatus.COMPLETED);
                pushScheduleRepository.save(schedule);
                expiredCount++;
                log.info("Marked schedule as expired: scheduleId={}, listingId={}",
                        schedule.getScheduleId(), schedule.getListingId());
            } catch (Exception e) {
                log.error("Error expiring schedule: scheduleId={}, error={}",
                        schedule.getScheduleId(), e.getMessage(), e);
            }
        }

        log.info("Expired {} schedules", expiredCount);
        return expiredCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PushHistory> getPushHistoryByListingId(Long listingId) {
        log.info("Fetching push history for listing: listingId={}", listingId);
        List<PushHistory> history = pushHistoryRepository.findByListingId(listingId);
        log.info("Found {} push history records for listing: listingId={}", history.size(), listingId);
        return history;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PushHistory> getPushHistoryByScheduleId(Long scheduleId) {
        log.info("Fetching push history for schedule: scheduleId={}", scheduleId);
        List<PushHistory> history = pushHistoryRepository.findByScheduleId(scheduleId);
        log.info("Found {} push history records for schedule: scheduleId={}", history.size(), scheduleId);
        return history;
    }

    /**
     * Helper method to create a push history record.
     * Encapsulates the history creation logic to avoid code duplication.
     *
     * @param scheduleId The schedule ID
     * @param listingId The listing ID
     * @param status The push status
     * @param message The message describing the push result
     * @param pushTime The time of the push
     */
    private void createPushHistory(Long scheduleId, Long listingId, PushHistory.PushStatus status,
                                    String message, LocalDateTime pushTime) {
        try {
            PushHistory history = PushHistory.builder()
                    .scheduleId(scheduleId)
                    .listingId(listingId)
                    .pushSource(PushHistory.PushSource.SCHEDULED)
                    .status(status)
                    .message(message)
                    .pushedAt(pushTime)
                    .build();

            pushHistoryRepository.save(history);
            log.debug("Created push history: scheduleId={}, listingId={}, status={}",
                    scheduleId, listingId, status);
        } catch (Exception e) {
            log.error("Failed to create push history: scheduleId={}, listingId={}, error={}",
                    scheduleId, listingId, e.getMessage(), e);
            // Don't throw exception to avoid disrupting the main push operation
        }
    }

    // ========== Boost Helper Methods ==========

    /**
     * Execute a single scheduled boost
     */
    private void executeScheduledBoost(PushSchedule schedule) {
        log.info("Executing scheduled boost: scheduleId={}, listingId={}",
                schedule.getScheduleId(), schedule.getListingId());

        // Check if schedule has remaining pushes
        if (!schedule.hasRemainingPushes()) {
            log.warn("Schedule {} has no remaining pushes", schedule.getScheduleId());
            schedule.setStatus(PushSchedule.ScheduleStatus.COMPLETED);
            pushScheduleRepository.save(schedule);
            return;
        }

        // Get listing
        Listing listing = listingRepository.findById(schedule.getListingId())
                .orElseThrow(() -> new RuntimeException("Listing not found: " + schedule.getListingId()));

        // If using membership quota, consume it
        if (schedule.getSource() == PushSchedule.PushSource.MEMBERSHIP) {
            boolean consumed = quotaService.consumeQuota(schedule.getUserId(), BenefitType.BOOST, 1);
            if (!consumed) {
                log.error("Failed to consume quota for schedule {}", schedule.getScheduleId());
                return;
            }
        }

        // Create push history
        PushHistory pushHistory = PushHistory.builder()
                .listingId(listing.getListingId())
                .pushSource(PushHistory.PushSource.SCHEDULED)
                .schedule(schedule)
                .transactionId(schedule.getTransactionId())
                .status(PushHistory.PushStatus.SUCCESS)
                .pushedAt(LocalDateTime.now())
                .build();

        // Update listing
        listing.setPushedAt(LocalDateTime.now());
        listing.setPostDate(LocalDateTime.now());
        listingRepository.save(listing);

        // Save history
        pushHistoryRepository.save(pushHistory);

        // Increment used pushes
        schedule.incrementUsedPushes();
        if (!schedule.hasRemainingPushes()) {
            schedule.setStatus(PushSchedule.ScheduleStatus.COMPLETED);
        }
        pushScheduleRepository.save(schedule);

        // If listing is Gold or Diamond, also boost shadow
        if ((listing.isGold() || listing.isDiamond()) && !listing.isShadowListing()) {
            boostShadowListing(listing, schedule.getUserId());
        }

        log.info("Successfully executed scheduled boost for listing {}", listing.getListingId());
    }

    /**
     * Boost the shadow listing associated with a Gold/Diamond listing
     */
    private void boostShadowListing(Listing mainListing, String userId) {
        try {
            // Find shadow listing
            Listing shadowListing = listingRepository.findShadowListingByMainListingId(mainListing.getListingId())
                    .orElse(null);

            if (shadowListing == null) {
                log.warn("No shadow listing found for main listing {}", mainListing.getListingId());
                return;
            }

            // Update shadow listing timestamps
            shadowListing.setPushedAt(LocalDateTime.now());
            shadowListing.setPostDate(LocalDateTime.now());
            listingRepository.save(shadowListing);

            // Create push history for shadow listing
            PushHistory shadowHistory = PushHistory.builder()
                    .listingId(shadowListing.getListingId())
                    .pushSource(PushHistory.PushSource.ADMIN) // Shadow boosts are automatic
                    .status(PushHistory.PushStatus.SUCCESS)
                    .pushedAt(LocalDateTime.now())
                    .build();
            pushHistoryRepository.save(shadowHistory);

            log.info("Boosted shadow listing {} for main listing {}",
                    shadowListing.getListingId(), mainListing.getListingId());
        } catch (Exception e) {
            log.error("Failed to boost shadow listing for {}: {}",
                    mainListing.getListingId(), e.getMessage());
        }
    }

    /**
     * Map PushHistory entity to BoostResponse DTO
     */
    private BoostResponse mapToBoostResponse(PushHistory pushHistory, String message) {
        return BoostResponse.builder()
                .listingId(pushHistory.getListingId())
                .pushSource(pushHistory.getPushSource() != null ? pushHistory.getPushSource().name() : null)
                .pushedAt(pushHistory.getPushedAt())
                .transactionId(pushHistory.getTransactionId())
                .message(message != null ? message : "Boost history retrieved")
                .build();
    }
}
