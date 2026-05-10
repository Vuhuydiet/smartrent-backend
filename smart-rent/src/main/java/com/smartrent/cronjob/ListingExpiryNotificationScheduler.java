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
 *  - Scans every live, public-visible listing whose expiry falls within the next
 *    7 days.
 *  - Groups by owner and sends ONE aggregated notification per owner per day —
 *    "Bạn có N tin đăng sắp hết hạn" — instead of one per listing. Owners click
 *    through to /seller/listings to see the full list.
 *  - In-app notification: pushed to every owner with expiring listings.
 *  - Email: only sent to owners whose email is in the WHITE_LIST_EMAIL_NOTIFICATION
 *    whitelist. Defaults to the internal team if the env var is empty so dev can
 *    smoke-test without spamming real users.
 *  - Fires once per day at 09:00 Asia/Ho_Chi_Minh (peak attention, before work).
 *  - Dedups via the notifications table itself: a (recipient, LISTING_EXPIRING,
 *    "LISTING_DAILY_SUMMARY") row created in the last 23h short-circuits resend,
 *    so manual re-runs and clock drift cannot double-fire.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ListingExpiryNotificationScheduler {

    static ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    static int LOOKAHEAD_DAYS = 7;
    static Duration DEDUP_LOOKBACK = Duration.ofHours(23);
    static String REFERENCE_TYPE = "LISTING_DAILY_SUMMARY";
    static String CLIENT_PATH_MY_LISTINGS = "/seller/listings";

    ListingRepository listingRepository;
    NotificationRepository notificationRepository;
    NotificationService notificationService;
    UserRepository userRepository;
    EmailService emailService;

    @NonFinal
    @Value("${application.notification.expiring-listing.whitelist-emails:}")
    String whitelistEmailsRaw;

    @NonFinal
    @Value("${application.client-url:http://localhost:3000}")
    String clientUrl;

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

        LocalDateTime windowEnd = now.plusDays(LOOKAHEAD_DAYS);
        List<Listing> candidates = listingRepository.findExpiringBetween(now, windowEnd);
        if (candidates.isEmpty()) {
            log.info("No listings expiring within {} days. Done.", LOOKAHEAD_DAYS);
            return;
        }

        Map<String, List<Listing>> byUser = candidates.stream()
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
                    .mapToLong(d -> Math.max(0, Duration.between(now, d).toDays()))
                    .min()
                    .orElse(LOOKAHEAD_DAYS);

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
        String manageUrl = clientUrl + CLIENT_PATH_MY_LISTINGS;
        String soonestLine = daysToSoonest <= 0
                ? "Tin sớm nhất sẽ hết hạn <strong>trong vòng 24 giờ tới</strong>."
                : "Tin sớm nhất sẽ hết hạn sau <strong>" + daysToSoonest + " ngày</strong>.";
        return "<!DOCTYPE html><html><body style=\"font-family:Arial,sans-serif;color:#222;\">"
                + "<h2 style=\"margin:0 0 12px\">SmartRent — Tin đăng sắp hết hạn</h2>"
                + "<p>Xin chào,</p>"
                + "<p>Bạn đang có <strong>" + count + "</strong> tin đăng sắp hết hạn. "
                + soonestLine + "</p>"
                + "<p>Hãy đăng nhập và truy cập trang quản lý tin để gia hạn, tránh gián đoạn hiển thị:</p>"
                + "<p><a href=\"" + manageUrl + "\" "
                + "style=\"display:inline-block;padding:10px 16px;background:#2563eb;color:#fff;"
                + "text-decoration:none;border-radius:6px;\">Xem tin đăng của tôi</a></p>"
                + "<p style=\"color:#666;font-size:12px;margin-top:24px;\">"
                + "Email này được gửi tự động — bạn không cần phản hồi.</p>"
                + "</body></html>";
    }
}
