package com.smartrent.cronjob;

import com.smartrent.enums.NotificationType;
import com.smartrent.enums.RecipientType;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.NotificationRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.service.notification.NotificationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Daily scheduler that warns listing owners ahead of expiry over WebSocket.
 *
 * Strategy:
 *  - Three milestones: D-7 (info), D-3 (warning), D-1 (urgent). Three is enough to
 *    keep owners aware without becoming spammy.
 *  - Fires once per day at 09:00 Asia/Ho_Chi_Minh (peak attention, before work).
 *  - Scans live, public-visible listings whose expiry falls inside any of the
 *    three milestone windows (±12h around the milestone instant).
 *  - Dedups via the notifications table itself: a (recipient, LISTING_EXPIRING,
 *    listingId, "LISTING_D{n}") row created in the last 23h short-circuits resend,
 *    so manual re-runs and clock drift cannot double-fire.
 *  - Reuses NotificationService → row in DB + push to /topic/notifications/{userId}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ListingExpiryNotificationScheduler {

    static ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    static int[] MILESTONES_DAYS = {7, 3, 1};
    static Duration MILESTONE_HALF_WINDOW = Duration.ofHours(12);
    static Duration DEDUP_LOOKBACK = Duration.ofHours(23);

    ListingRepository listingRepository;
    NotificationRepository notificationRepository;
    NotificationService notificationService;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Ho_Chi_Minh")
    public void notifyExpiringListings() {
        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        log.info("=== Starting expiring-listing notification scan at {} (SGT) ===", now);

        int totalSent = 0;
        for (int days : MILESTONES_DAYS) {
            try {
                totalSent += processMilestone(now, days);
            } catch (Exception e) {
                log.error("Failed processing D-{} milestone: {}", days, e.getMessage(), e);
            }
        }
        log.info("=== Expiring-listing notification scan done. Sent {} notifications ===", totalSent);
    }

    private int processMilestone(LocalDateTime now, int days) {
        LocalDateTime target = now.plusDays(days);
        LocalDateTime windowStart = target.minus(MILESTONE_HALF_WINDOW);
        LocalDateTime windowEnd = target.plus(MILESTONE_HALF_WINDOW);

        List<Listing> candidates = listingRepository.findExpiringBetween(windowStart, windowEnd);
        if (candidates.isEmpty()) {
            log.debug("D-{}: no listings in window [{}, {}]", days, windowStart, windowEnd);
            return 0;
        }

        String referenceType = "LISTING_D" + days;
        LocalDateTime dedupAfter = now.minus(DEDUP_LOOKBACK);
        int sent = 0;

        for (Listing l : candidates) {
            if (l.getUserId() == null || l.getUserId().isBlank()) continue;

            boolean alreadySent = notificationRepository
                    .existsByRecipientIdAndRecipientTypeAndTypeAndReferenceIdAndReferenceTypeAndCreatedAtAfter(
                            l.getUserId(),
                            RecipientType.USER,
                            NotificationType.LISTING_EXPIRING,
                            l.getListingId(),
                            referenceType,
                            dedupAfter);
            if (alreadySent) continue;

            notificationService.sendNotification(
                    l.getUserId(),
                    RecipientType.USER,
                    NotificationType.LISTING_EXPIRING,
                    buildTitle(days),
                    buildMessage(l, days),
                    l.getListingId(),
                    referenceType);
            sent++;
        }

        log.info("D-{}: scanned {} listings, sent {} notifications", days, candidates.size(), sent);
        return sent;
    }

    private static String buildTitle(int days) {
        return switch (days) {
            case 1 -> "Tin đăng sắp hết hạn trong 24h";
            case 3 -> "Tin đăng còn 3 ngày sẽ hết hạn";
            default -> "Tin đăng sắp hết hạn trong 7 ngày";
        };
    }

    private static String buildMessage(Listing l, int days) {
        String title = l.getTitle() == null ? "(không tiêu đề)" : l.getTitle();
        return switch (days) {
            case 1 -> "Tin \"" + title + "\" sẽ hết hạn trong vòng 24 giờ tới. "
                    + "Hãy gia hạn ngay để tránh gián đoạn hiển thị.";
            case 3 -> "Tin \"" + title + "\" còn 3 ngày là hết hạn. "
                    + "Bạn nên gia hạn sớm để giữ thứ hạng hiển thị.";
            default -> "Tin \"" + title + "\" sẽ hết hạn sau 7 ngày. "
                    + "Hãy chuẩn bị gia hạn hoặc cập nhật nội dung nếu cần.";
        };
    }
}
