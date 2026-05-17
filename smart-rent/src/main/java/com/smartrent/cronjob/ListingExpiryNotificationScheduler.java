package com.smartrent.cronjob;

import com.smartrent.enums.NotificationType;
import com.smartrent.enums.RecipientType;
import com.smartrent.infra.connector.model.EmailInfo;
import com.smartrent.infra.connector.model.EmailRequest;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.NotificationRepository;
import com.smartrent.infra.repository.UserRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.User;
import com.smartrent.service.email.EmailService;
import com.smartrent.service.notification.NotificationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Daily scheduler that warns listing owners ahead of expiry.
 *
 * Strategy:
 *  - Runs once per day but only notifies an owner at TWO milestones: when a
 *    listing is exactly 3 calendar days from expiry, and again when it is
 *    exactly 1 day from expiry (the last full day). No daily spam in between.
 *  - Groups by owner and sends ONE aggregated notification per owner per run —
 *    "Bạn có N tin đăng sắp hết hạn" — instead of one per listing. Owners click
 *    through to /seller/listings to see the full list.
 *  - In-app notification: pushed to every owner with a listing hitting a milestone.
 *  - Email: only sent to owners whose email is in the WHITE_LIST_EMAIL_NOTIFICATION
 *    whitelist. Defaults to the internal team if the env var is empty so dev can
 *    smoke-test without spamming real users.
 *  - Fires once per day at 09:00 Asia/Ho_Chi_Minh (peak attention, before work).
 *  - Dedups via the notifications table itself: a (recipient, LISTING_EXPIRING,
 *    "LISTING_DAILY_SUMMARY") row created in the last 23h short-circuits resend,
 *    so manual re-runs and clock drift cannot double-fire. The 3-day and 1-day
 *    milestones are 2 days apart, so the 23h window never blocks the second one.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ListingExpiryNotificationScheduler {

    static ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    /** Only notify when a listing is exactly this many calendar days from expiry. */
    static Set<Long> NOTIFY_DAY_OFFSETS = Set.of(3L, 1L);
    /**
     * DB window must reach the far edge of the largest milestone. The job runs at
     * 09:00, but a listing 3 calendar days out can expire as late as ~23:59 that
     * day, so we query 4 days ahead and filter down to the exact milestones below.
     */
    static int QUERY_LOOKAHEAD_DAYS = 4;
    static Duration DEDUP_LOOKBACK = Duration.ofHours(23);
    static String REFERENCE_TYPE = "LISTING_DAILY_SUMMARY";

    ListingRepository listingRepository;
    NotificationRepository notificationRepository;
    NotificationService notificationService;
    UserRepository userRepository;
    EmailService emailService;

    @NonFinal
    @Value("${application.notification.expiring-listing.whitelist-emails:}")
    String whitelistEmailsRaw;

    @NonFinal
    @Value("${application.client-url}")
    String clientUrl;

    @NonFinal
    @Value("${application.notification.expiring-listing.manage-path:/seller/listings?page=1&size=10}")
    String managePath;

    @NonFinal
    @Value("${application.email.sender.email}")
    String senderEmail;

    @NonFinal
    @Value("${application.email.sender.name:SmartRent}")
    String senderName;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Ho_Chi_Minh")
    public void notifyExpiringListings() {
        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        log.info("=== Starting expiring-listing notification scan at {} (SGT) ===", now);

        LocalDateTime windowEnd = now.plusDays(QUERY_LOOKAHEAD_DAYS);
        List<Listing> candidates = listingRepository.findExpiringBetween(now, windowEnd);
        if (candidates.isEmpty()) {
            log.info("No listings expiring within {} days. Done.", QUERY_LOOKAHEAD_DAYS);
            return;
        }

        // Keep only listings sitting exactly on a milestone (3 or 1 day out) so
        // owners are pinged at those two points, not every single day.
        List<Listing> dueForReminder = candidates.stream()
                .filter(l -> l.getExpiryDate() != null
                        && NOTIFY_DAY_OFFSETS.contains(calendarDaysUntil(now, l.getExpiryDate())))
                .collect(Collectors.toList());
        if (dueForReminder.isEmpty()) {
            log.info("{} listings expiring soon but none at a {}-day milestone. Done.",
                    candidates.size(), NOTIFY_DAY_OFFSETS);
            return;
        }

        Map<String, List<Listing>> byUser = dueForReminder.stream()
                .filter(l -> l.getUserId() != null && !l.getUserId().isBlank())
                .collect(Collectors.groupingBy(Listing::getUserId));
        if (byUser.isEmpty()) {
            log.info("All {} expiring listings have no owner; skipping.", candidates.size());
            return;
        }

        Map<String, String> userEmails = userRepository.findAllById(byUser.keySet()).stream()
                .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
                .collect(Collectors.toMap(User::getUserId, User::getEmail));

        Set<String> whitelist = parseWhitelist();
        LocalDateTime dedupAfter = now.minus(DEDUP_LOOKBACK);

        int notified = 0;
        int emailed = 0;
        for (Map.Entry<String, List<Listing>> entry : byUser.entrySet()) {
            String userId = entry.getKey();
            List<Listing> listings = entry.getValue();

            boolean alreadySent = notificationRepository
                    .existsByRecipientIdAndRecipientTypeAndTypeAndReferenceTypeAndCreatedAtAfter(
                            userId,
                            RecipientType.USER,
                            NotificationType.LISTING_EXPIRING,
                            REFERENCE_TYPE,
                            dedupAfter);
            if (alreadySent) {
                log.debug("User {} already received daily summary within {}h, skipping.",
                        userId, DEDUP_LOOKBACK.toHours());
                continue;
            }

            int count = listings.size();
            long daysToSoonest = listings.stream()
                    .map(Listing::getExpiryDate)
                    .filter(d -> d != null)
                    .mapToLong(d -> calendarDaysUntil(now, d))
                    .min()
                    .orElse(1);

            notificationService.sendNotification(
                    userId,
                    RecipientType.USER,
                    NotificationType.LISTING_EXPIRING,
                    buildTitle(count),
                    buildMessage(count, daysToSoonest),
                    null,
                    REFERENCE_TYPE);
            notified++;

            String email = userEmails.get(userId);
            if (email != null && whitelist.contains(email.toLowerCase())) {
                if (sendSummaryEmail(email, count, daysToSoonest)) {
                    emailed++;
                }
            }
        }

        log.info("=== Expiring-listing scan done. Owners={}, notified={}, emailed={} ===",
                byUser.size(), notified, emailed);
    }

    /**
     * Whole calendar days from {@code now}'s date to the expiry date, ignoring
     * time-of-day. Using the date (not raw Duration) keeps the milestone stable
     * regardless of what hour the job runs or the listing happens to expire.
     */
    private static long calendarDaysUntil(LocalDateTime now, LocalDateTime expiry) {
        return ChronoUnit.DAYS.between(now.toLocalDate(), expiry.toLocalDate());
    }

    private boolean sendSummaryEmail(String to, int count, long daysToSoonest) {
        try {
            EmailRequest request = EmailRequest.builder()
                    .sender(EmailInfo.builder().email(senderEmail).name(senderName).build())
                    .to(List.of(EmailInfo.builder().email(to).build()))
                    .subject(buildEmailSubject(count))
                    .htmlContent(buildEmailHtml(count, daysToSoonest))
                    .build();
            emailService.sendEmail(request);
            return true;
        } catch (Exception e) {
            log.warn("Failed to send expiring-listing summary email to {}: {}", to, e.getMessage());
            return false;
        }
    }

    private Set<String> parseWhitelist() {
        if (whitelistEmailsRaw == null || whitelistEmailsRaw.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(whitelistEmailsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private static String buildTitle(int count) {
        return "Bạn có " + count + " tin đăng sắp hết hạn";
    }

    private static String buildMessage(int count, long daysToSoonest) {
        if (daysToSoonest <= 0) {
            return "Bạn có " + count + " tin đăng sẽ hết hạn trong vòng 24 giờ tới. "
                    + "Hãy vào trang quản lý tin để gia hạn ngay.";
        }
        return "Bạn có " + count + " tin đăng sắp hết hạn (sớm nhất sau " + daysToSoonest
                + " ngày). Hãy vào trang quản lý tin để xem và gia hạn.";
    }

    private static String buildEmailSubject(int count) {
        return "[SmartRent] Bạn có " + count + " tin đăng sắp hết hạn";
    }

    private String buildEmailHtml(int count, long daysToSoonest) {
        String manageUrl = buildManageUrl();
        String soonestLine = daysToSoonest <= 0
                ? "Tin sớm nhất sẽ hết hạn <strong>trong vòng 24 giờ tới</strong>."
                : "Tin sớm nhất sẽ hết hạn sau <strong>" + daysToSoonest + " ngày</strong>.";
        return "<!DOCTYPE html><html><body style=\"font-family:Arial,sans-serif;color:#222;\">"
                + "<h2 style=\"margin:0 0 12px\">SmartRent — Tin đăng sắp hết hạn</h2>"
                + "<p>Xin chào,</p>"
                + "<p>Bạn đang có <strong>" + count + "</strong> tin đăng sắp hết hạn. "
                + soonestLine + "</p>"
                + "<p>Sau đó vui lòng vào trang này để gia hạn hoặc đăng lại:</p>"
                + "<p><a href=\"" + manageUrl + "\" "
                + "style=\"display:inline-block;padding:10px 16px;background:#2563eb;color:#fff;"
                + "text-decoration:none;border-radius:6px;\">Quản lý tin đăng của tôi</a></p>"
                + "<p style=\"font-size:13px;color:#555;\">Hoặc mở liên kết: "
                + "<a href=\"" + manageUrl + "\">" + manageUrl + "</a></p>"
                + "<p style=\"color:#666;font-size:12px;margin-top:24px;\">"
                + "Email này được gửi tự động — bạn không cần phản hồi.</p>"
                + "</body></html>";
    }

    private String buildManageUrl() {
        String base = clientUrl == null ? "" : clientUrl.trim();
        String path = managePath == null ? "" : managePath.trim();
        if (base.endsWith("/") && path.startsWith("/")) {
            return base.substring(0, base.length() - 1) + path;
        }
        if (!base.endsWith("/") && !path.startsWith("/") && !path.isEmpty()) {
            return base + "/" + path;
        }
        return base + path;
    }
}
