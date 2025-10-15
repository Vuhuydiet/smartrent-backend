package com.smartrent.service.push.impl;

import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.PushHistoryRepository;
import com.smartrent.infra.repository.PushScheduleRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.PushHistory;
import com.smartrent.infra.repository.entity.PushSchedule;
import com.smartrent.service.push.PushService;
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

/**
 * Implementation of PushService.
 * Handles listing push operations with proper transaction management and error handling.
 * Follows SOLID principles with single responsibility for push operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PushServiceImpl implements PushService {

    ListingRepository listingRepository;
    PushScheduleRepository pushScheduleRepository;
    PushHistoryRepository pushHistoryRepository;

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
                .endTime(endTime)
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
                schedule.setStatus(PushSchedule.ScheduleStatus.EXPIRED);
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
}
