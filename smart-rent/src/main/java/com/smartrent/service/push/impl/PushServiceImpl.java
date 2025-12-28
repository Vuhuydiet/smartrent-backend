package com.smartrent.service.push.impl;

import com.smartrent.constants.PricingConstants;
import com.smartrent.dto.request.PushListingRequest;
import com.smartrent.dto.request.PaymentRequest;
import com.smartrent.dto.request.SchedulePushRequest;
import com.smartrent.dto.response.PageResponse;
import com.smartrent.dto.response.PushResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of PushService.
 * Handles listing push operations with proper transaction management and error handling.
 * Consolidated service for both immediate pushes and scheduled pushes.
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

    // ========== Push Listing Methods ==========

    @Override
    @Transactional
    public PushResponse pushListing(String userId, PushListingRequest request) {
        log.info("Pushing listing {} for user {}", request.getListingId(), userId);

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
            var quotaStatus = quotaService.checkQuotaAvailability(userId, BenefitType.PUSH);

            if (quotaStatus.getTotalAvailable() > 0) {
                // Use membership quota
                boolean consumed = quotaService.consumeQuota(userId, BenefitType.PUSH, 1);
                if (!consumed) {
                    throw new RuntimeException("Failed to consume push quota");
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

                // If listing is Gold or Diamond, also push its shadow listing
                if ((listing.isGold() || listing.isDiamond()) && !listing.isShadowListing()) {
                    pushShadowListing(listing, userId);
                }

                log.info("Successfully pushed listing {} using quota", request.getListingId());
                return mapToPushResponse(pushHistory, "Listing pushed successfully using quota");
            }
        }

        // No quota or user chose not to use quota - require payment
        log.info("No quota available or user chose payment - initiating payment flow for push");

        // Create PENDING transaction for push
        String transactionId = transactionService.createPushFeeTransaction(
                userId,
                request.getListingId(),
                PricingConstants.PUSH_PER_TIME,
                request.getPaymentProvider() != null ? request.getPaymentProvider() : "VNPAY"
        );

        // Generate payment URL - pass transactionId to reuse existing transaction
        // Use simple English orderInfo to avoid VNPay encoding issues
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .transactionId(transactionId) // Reuse the transaction created above
                .provider(PaymentProvider.valueOf(
                        request.getPaymentProvider() != null ? request.getPaymentProvider() : "VNPAY"))
                .amount(PricingConstants.PUSH_PER_TIME)
                .currency(PricingConstants.DEFAULT_CURRENCY)
                .orderInfo("SmartRent Push Listing " + request.getListingId())
                .build();

        PaymentResponse paymentResponse = paymentService.createPayment(paymentRequest, null);

        log.info("Payment URL generated for push transaction: {}", transactionId);

        // Return payment response wrapped in PushResponse
        return PushResponse.builder()
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
    public PushResponse completePushAfterPayment(String transactionId) {
        log.info("Completing push after payment for transaction: {}", transactionId);

        // Get transaction
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        if (!transaction.isCompleted()) {
            throw new RuntimeException("Transaction is not completed: " + transactionId);
        }

        if (!transaction.isPushFee()) {
            throw new RuntimeException("Transaction is not a push fee: " + transactionId);
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

        // If listing is Gold or Diamond, also push its shadow listing
        if ((listing.isGold() || listing.isDiamond()) && !listing.isShadowListing()) {
            pushShadowListing(listing, transaction.getUserId());
        }

        log.info("Successfully pushed listing {} after payment", listingId);
        return mapToPushResponse(pushHistory, "Listing pushed successfully after payment");
    }

    @Override
    @Transactional
    public PushResponse schedulePush(String userId, SchedulePushRequest request) {
        log.info("Scheduling push for listing {} at time {}", request.getListingId(), request.getScheduledTime());

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
                    userId, BenefitType.PUSH, LocalDateTime.now());

            if (availableQuota == null || availableQuota < request.getTotalPushes()) {
                throw new RuntimeException("Insufficient push quota. Available: " + availableQuota +
                        ", Required: " + request.getTotalPushes());
            }

            source = PushSchedule.PushSource.MEMBERSHIP;
        } else {
            // Direct purchase - create transaction
            String txnId = transactionService.createPushFeeTransaction(
                    userId,
                    request.getListingId(),
                    PricingConstants.PUSH_PER_TIME.multiply(java.math.BigDecimal.valueOf(request.getTotalPushes())),
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

        log.info("Successfully scheduled push for listing {}", request.getListingId());
        return PushResponse.builder()
                .listingId(request.getListingId())
                .userId(userId)
                .pushSource(source.name())
                .message("Push scheduled successfully for " + request.getScheduledTime())
                .build();
    }

    @Override
    @Transactional
    public int executeScheduledPushes() {
        log.info("Executing scheduled pushes");
        LocalTime currentTime = LocalTime.now().withSecond(0).withNano(0);

        List<PushSchedule> schedules = pushScheduleRepository.findActiveSchedulesByTime(currentTime);
        int executedCount = 0;

        for (PushSchedule schedule : schedules) {
            try {
                executeScheduledPush(schedule);
                executedCount++;
            } catch (Exception e) {
                log.error("Failed to execute scheduled push for schedule {}: {}",
                        schedule.getScheduleId(), e.getMessage());
            }
        }

        log.info("Executed {} scheduled pushes", executedCount);
        return executedCount;
    }

    @Override
    @Transactional
    public void cancelScheduledPush(String userId, Long scheduleId) {
        log.info("Cancelling scheduled push {} for user {}", scheduleId, userId);

        PushSchedule schedule = pushScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found: " + scheduleId));

        if (!schedule.getUserId().equals(userId)) {
            throw new RuntimeException("Schedule does not belong to user");
        }

        schedule.cancel();
        pushScheduleRepository.save(schedule);

        log.info("Successfully cancelled scheduled push {}", scheduleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PushResponse> getPushHistory(Long listingId) {
        log.info("Getting push history for listing: {}", listingId);
        return pushHistoryRepository.findByListingIdOrderByPushedAtDesc(listingId)
                .stream()
                .map(ph -> mapToPushResponse(ph, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PushResponse> getPushHistory(Long listingId, int page, int size) {
        log.info("Getting push history for listing: {} with pagination - page: {}, size: {}", listingId, page, size);

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<PushHistory> pushHistoryPage = pushHistoryRepository.findAll(pageable);

        // Filter by listingId
        List<PushResponse> pushResponses = pushHistoryPage.getContent().stream()
                .filter(ph -> ph.getListingId().equals(listingId))
                .map(ph -> mapToPushResponse(ph, null))
                .collect(Collectors.toList());

        log.info("Successfully retrieved {} push history records", pushResponses.size());

        return PageResponse.<PushResponse>builder()
                .page(page)
                .size(pushHistoryPage.getSize())
                .totalPages(pushHistoryPage.getTotalPages())
                .totalElements(pushHistoryPage.getTotalElements())
                .data(pushResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PushResponse> getUserPushHistory(String userId) {
        log.info("Getting push history for user: {}", userId);

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
                .map(ph -> mapToPushResponse(ph, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PushResponse> getUserPushHistory(String userId, int page, int size) {
        log.info("Getting push history for user: {} with pagination - page: {}, size: {}", userId, page, size);

        // Find all listings owned by the user
        List<Listing> userListings = listingRepository.findByUserId(userId);
        List<Long> listingIds = userListings.stream()
                .map(Listing::getListingId)
                .collect(Collectors.toList());

        if (listingIds.isEmpty()) {
            log.info("No listings found for user: {}", userId);
            return PageResponse.<PushResponse>builder()
                    .page(page)
                    .size(size)
                    .totalPages(0)
                    .totalElements(0L)
                    .data(List.of())
                    .build();
        }

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<PushHistory> pushHistoryPage = pushHistoryRepository.findAll(pageable);

        // Filter by user's listing IDs
        List<PushResponse> pushResponses = pushHistoryPage.getContent().stream()
                .filter(ph -> listingIds.contains(ph.getListingId()))
                .map(ph -> mapToPushResponse(ph, null))
                .collect(Collectors.toList());

        log.info("Successfully retrieved {} push history records for user", pushResponses.size());

        return PageResponse.<PushResponse>builder()
                .page(page)
                .size(pushHistoryPage.getSize())
                .totalPages(pushHistoryPage.getTotalPages())
                .totalElements(pushHistoryPage.getTotalElements())
                .data(pushResponses)
                .build();
    }

    // ========== Helper Methods ==========

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
     * Execute a single scheduled push
     */
    private void executeScheduledPush(PushSchedule schedule) {
        log.info("Executing scheduled push: scheduleId={}, listingId={}",
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
            boolean consumed = quotaService.consumeQuota(schedule.getUserId(), BenefitType.PUSH, 1);
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

        // If listing is Gold or Diamond, also push shadow
        if ((listing.isGold() || listing.isDiamond()) && !listing.isShadowListing()) {
            pushShadowListing(listing, schedule.getUserId());
        }

        log.info("Successfully executed scheduled push for listing {}", listing.getListingId());
    }

    /**
     * Push the shadow listing associated with a Gold/Diamond listing
     */
    private void pushShadowListing(Listing mainListing, String userId) {
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
                    .pushSource(PushHistory.PushSource.ADMIN) // Shadow pushes are automatic
                    .status(PushHistory.PushStatus.SUCCESS)
                    .pushedAt(LocalDateTime.now())
                    .build();
            pushHistoryRepository.save(shadowHistory);

            log.info("Pushed shadow listing {} for main listing {}",
                    shadowListing.getListingId(), mainListing.getListingId());
        } catch (Exception e) {
            log.error("Failed to push shadow listing for {}: {}",
                    mainListing.getListingId(), e.getMessage());
        }
    }

    /**
     * Map PushHistory entity to PushResponse DTO
     */
    private PushResponse mapToPushResponse(PushHistory pushHistory, String message) {
        return PushResponse.builder()
                .listingId(pushHistory.getListingId())
                .pushSource(pushHistory.getPushSource() != null ? pushHistory.getPushSource().name() : null)
                .pushedAt(pushHistory.getPushedAt())
                .transactionId(pushHistory.getTransactionId())
                .message(message != null ? message : "Push history retrieved")
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<PushHistory> getPushHistoryByUserId(String userId) {
        log.info("Getting push history entities for user: {}", userId);

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
        List<PushHistory> history = pushHistoryRepository.findByListingIdInOrderByPushedAtDesc(listingIds);
        log.info("Found {} push history records for user: {}", history.size(), userId);
        return history;
    }
}
