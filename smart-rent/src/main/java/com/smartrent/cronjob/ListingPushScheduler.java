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
     * Execute scheduled pushes at the start of every hour.
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
    public void executeScheduledPushes() {
        log.info("=== Starting scheduled push execution ===");

        try {
            int pushedCount = pushService.executeScheduledPushes();
            log.info("=== Completed scheduled push execution. Pushed {} listings ===", pushedCount);
        } catch (Exception e) {
            log.error("=== Error during scheduled push execution: {} ===", e.getMessage(), e);
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
