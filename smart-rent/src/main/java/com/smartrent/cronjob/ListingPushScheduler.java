package com.smartrent.cronjob;

import com.smartrent.service.push.PushService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Scheduler component for processing listing push schedules.
 * Runs at the start of every hour to process active push schedules.
 * Also runs a cleanup task to expire old schedules.
 *
 * Uses Spring's @Scheduled annotation for declarative scheduling.
 * Follows Single Responsibility Principle - only handles scheduling logic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ListingPushScheduler {

    PushService pushService;

    /**
     * Process scheduled pushes at the start of every hour.
     * Cron expression: "0 0 * * * *" means:
     * - Second: 0 (at the start of the minute)
     * - Minute: 0 (at the start of the hour)
     * - Hour: * (every hour)
     * - Day of month: * (every day)
     * - Month: * (every month)
     * - Day of week: * (every day of week)
     *
     * This will run at 00:00, 01:00, 02:00, etc.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void processScheduledPushes() {
        LocalDateTime currentTime = LocalDateTime.now();
        log.info("=== Starting scheduled push processing at {} ===", currentTime);

        try {
            int pushedCount = pushService.processScheduledPushes(currentTime);
            log.info("=== Completed scheduled push processing. Pushed {} listings ===", pushedCount);
        } catch (Exception e) {
            log.error("=== Error during scheduled push processing: {} ===", e.getMessage(), e);
        }
    }

    /**
     * Cleanup task to mark expired schedules.
     * Runs every day at 1:00 AM to clean up schedules that have passed their end_time.
     * Cron expression: "0 0 1 * * *" means:
     * - Second: 0
     * - Minute: 0
     * - Hour: 1 (1 AM)
     * - Day of month: * (every day)
     * - Month: * (every month)
     * - Day of week: * (every day of week)
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void cleanupExpiredSchedules() {
        LocalDateTime currentTime = LocalDateTime.now();
        log.info("=== Starting cleanup of expired schedules at {} ===", currentTime);

        try {
            int expiredCount = pushService.expireOldSchedules(currentTime);
            log.info("=== Completed cleanup. Expired {} schedules ===", expiredCount);
        } catch (Exception e) {
            log.error("=== Error during cleanup of expired schedules: {} ===", e.getMessage(), e);
        }
    }

    /**
     * Health check task that logs scheduler status.
     * Runs every 6 hours to confirm the scheduler is running.
     * This helps with monitoring and debugging in production.
     * Cron expression: "0 0 * /6 * * *" means every 6 hours
     */
    @Scheduled(cron = "0 0 */6 * * *")
    public void healthCheck() {
        log.info("=== ListingPushScheduler is active and running ===");
    }
}
