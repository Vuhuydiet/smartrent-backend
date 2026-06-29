package com.smartrent.cronjob;

import com.smartrent.enums.NotificationType;
import com.smartrent.enums.RecipientType;
import com.smartrent.infra.connector.model.EmailInfo;
import com.smartrent.infra.connector.model.EmailRequest;
import com.smartrent.infra.repository.NotificationRepository;
import com.smartrent.infra.repository.UserRepository;
import com.smartrent.infra.repository.UserMembershipRepository;
import com.smartrent.infra.repository.entity.User;
import com.smartrent.infra.repository.entity.UserMembership;
import com.smartrent.service.email.EmailService;
import com.smartrent.service.notification.NotificationService;
import com.smartrent.utility.EmailBuilder;
import com.smartrent.utility.Utils;
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
 * Daily scheduler that warns membership holders ahead of package expiry.
 *
 * Strategy:
 *  - Queries ACTIVE memberships ending within QUERY_LOOKAHEAD_DAYS (8) days.
 *  - Only notifies at TWO milestones: D-7 and D-3, to avoid daily spam.
 *  - In-app notification pushed to every affected member.
 *  - Email only sent to members whose email is in the WHITE_LIST_EMAIL_NOTIFICATION whitelist.
 *  - Deduplicates via the notifications table (23-hour window).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MembershipExpiryNotificationScheduler {

    static ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    static Set<Long> NOTIFY_DAY_OFFSETS = Set.of(7L, 3L);
    static int QUERY_LOOKAHEAD_DAYS = 8;
    static Duration DEDUP_LOOKBACK = Duration.ofHours(23);
    static String REFERENCE_TYPE = "MEMBERSHIP_DAILY_SUMMARY";

    UserMembershipRepository userMembershipRepository;
    NotificationRepository notificationRepository;
    NotificationService notificationService;
    UserRepository userRepository;
    EmailService emailService;

    @NonFinal
    @Value("${application.notification.expiring-membership.whitelist-emails:}")
    String whitelistEmailsRaw;

    @NonFinal
    @Value("${application.client-url}")
    String clientUrl;

    @NonFinal
    @Value("${application.notification.expiring-membership.manage-path:/buyer/memberships}")
    String managePath;

    @NonFinal
    @Value("${application.email.sender.email}")
    String senderEmail;

    @NonFinal
    @Value("${application.email.sender.name:SmartRent}")
    String senderName;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Ho_Chi_Minh")
    public void notifyExpiringMemberships() {
        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        log.info("=== Starting expiring-membership notification scan at {} (SGT) ===", now);

        LocalDateTime windowEnd = now.plusDays(QUERY_LOOKAHEAD_DAYS);
        List<UserMembership> candidates = userMembershipRepository.findExpiringBetween(now, windowEnd);
        if (candidates.isEmpty()) {
            log.info("No memberships expiring within {} days. Done.", QUERY_LOOKAHEAD_DAYS);
            return;
        }

        List<UserMembership> dueForReminder = candidates.stream()
                .filter(um -> NOTIFY_DAY_OFFSETS.contains(calendarDaysUntil(now, um.getEndDate())))
                .collect(Collectors.toList());
        if (dueForReminder.isEmpty()) {
            log.info("{} memberships expiring soon but none at a {}-day milestone. Done.",
                    candidates.size(), NOTIFY_DAY_OFFSETS);
            return;
        }

        Map<String, UserMembership> byUser = dueForReminder.stream()
                .filter(um -> um.getUserId() != null && !um.getUserId().isBlank())
                .collect(Collectors.toMap(UserMembership::getUserId, um -> um, (a, b) -> a));

        Map<String, User> usersById = userRepository.findAllById(byUser.keySet()).stream()
                .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
                .collect(Collectors.toMap(User::getUserId, u -> u));

        Set<String> whitelist = parseWhitelist();
        LocalDateTime dedupAfter = now.minus(DEDUP_LOOKBACK);

        int notified = 0;
        int emailed = 0;
        for (Map.Entry<String, UserMembership> entry : byUser.entrySet()) {
            String userId = entry.getKey();
            UserMembership membership = entry.getValue();

            boolean alreadySent = notificationRepository
                    .existsByRecipientIdAndRecipientTypeAndTypeAndReferenceTypeAndCreatedAtAfter(
                            userId,
                            RecipientType.USER,
                            NotificationType.MEMBERSHIP_EXPIRING,
                            REFERENCE_TYPE,
                            dedupAfter);
            if (alreadySent) {
                log.debug("User {} already received membership expiry summary within {}h, skipping.",
                        userId, DEDUP_LOOKBACK.toHours());
                continue;
            }

            long daysRemaining = calendarDaysUntil(now, membership.getEndDate());
            String packageName = membership.getMembershipPackage().getPackageName();

            notificationService.sendNotification(
                    userId,
                    RecipientType.USER,
                    NotificationType.MEMBERSHIP_EXPIRING,
                    buildTitle(packageName, daysRemaining),
                    buildMessage(packageName, daysRemaining),
                    null,
                    REFERENCE_TYPE);
            notified++;

            User user = usersById.get(userId);
            if (user != null && whitelist.contains(user.getEmail().toLowerCase())) {
                if (sendExpiryEmail(user, packageName, daysRemaining, membership.getEndDate())) {
                    emailed++;
                }
            }
        }

        log.info("=== Expiring-membership scan done. Members={}, notified={}, emailed={} ===",
                byUser.size(), notified, emailed);
    }

    private static long calendarDaysUntil(LocalDateTime now, LocalDateTime expiry) {
        return ChronoUnit.DAYS.between(now.toLocalDate(), expiry.toLocalDate());
    }

    private boolean sendExpiryEmail(User user, String packageName, long daysRemaining,
                                    LocalDateTime endDate) {
        String to = user.getEmail();
        try {
            EmailInfo recipient = EmailInfo.builder()
                    .email(to)
                    .name(Utils.buildName(user.getFirstName(), user.getLastName()))
                    .build();
            EmailRequest request = EmailRequest.builder()
                    .sender(EmailInfo.builder().email(senderEmail).name(senderName).build())
                    .to(List.of(recipient))
                    .subject(buildEmailSubject(packageName, daysRemaining))
                    .htmlContent(EmailBuilder.buildExpiringMembershipHtmlContent(
                            senderName,
                            user.getFirstName(),
                            user.getLastName(),
                            packageName,
                            daysRemaining,
                            endDate,
                            buildManageUrl()))
                    .build();
            emailService.sendEmail(request);
            return true;
        } catch (Exception e) {
            log.warn("Failed to send membership expiry email to {}: {}", to, e.getMessage());
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

    private static String buildTitle(String packageName, long daysRemaining) {
        return "Gói thành viên " + packageName + " sắp hết hạn";
    }

    private static String buildMessage(String packageName, long daysRemaining) {
        if (daysRemaining <= 0) {
            return "Gói thành viên " + packageName + " của bạn sẽ hết hạn trong vòng 24 giờ tới. "
                    + "Hãy gia hạn ngay để tiếp tục sử dụng dịch vụ.";
        }
        return "Gói thành viên " + packageName + " của bạn còn " + daysRemaining
                + " ngày nữa sẽ hết hạn. Hãy gia hạn sớm để không bị gián đoạn dịch vụ.";
    }

    private static String buildEmailSubject(String packageName, long daysRemaining) {
        return "[SmartRent] Gói thành viên " + packageName + " sắp hết hạn";
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
