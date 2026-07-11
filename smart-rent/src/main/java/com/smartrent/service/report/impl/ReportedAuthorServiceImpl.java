package com.smartrent.service.report.impl;

import com.smartrent.config.Constants;
import com.smartrent.dto.request.PostingBlockRequest;
import com.smartrent.dto.response.ListingReportResponse;
import com.smartrent.dto.response.PageResponse;
import com.smartrent.dto.response.ReportedAuthorResponse;
import com.smartrent.enums.NotificationType;
import com.smartrent.enums.RecipientType;
import com.smartrent.enums.ReportStatus;
import com.smartrent.infra.exception.UserNotBlockEligibleException;
import com.smartrent.infra.exception.UserNotFoundException;
import com.smartrent.infra.repository.AdminRepository;
import com.smartrent.infra.repository.ListingReportRepository;
import com.smartrent.infra.repository.ReportedAuthorProjection;
import com.smartrent.infra.repository.UserRepository;
import com.smartrent.infra.repository.entity.ListingReport;
import com.smartrent.infra.repository.entity.User;
import com.smartrent.mapper.ListingReportMapper;
import com.smartrent.service.notification.NotificationService;
import com.smartrent.service.report.ReportedAuthorService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReportedAuthorServiceImpl implements ReportedAuthorService {

    UserRepository userRepository;
    ListingReportRepository listingReportRepository;
    ListingReportMapper listingReportMapper;
    AdminRepository adminRepository;
    NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReportedAuthorResponse> getReportedAuthors(
            String email, String name, String phone, Boolean blockEligible, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = size < 1 ? 20 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize);

        // Sentinels avoid binding nulls in the native query
        String emailFilter = email != null ? email.trim() : "";
        String nameFilter = name != null ? name.trim() : "";
        String phoneFilter = phone != null ? phone.trim() : "";
        int blockEligibleFlag = blockEligible == null ? -1 : (blockEligible ? 1 : 0);

        Page<ReportedAuthorProjection> authorPage = listingReportRepository.findReportedAuthors(
                emailFilter, nameFilter, phoneFilter, blockEligibleFlag, BLOCK_ELIGIBLE_THRESHOLD, pageable);

        // Batch-load user details for the page
        List<String> userIds = authorPage.getContent().stream()
                .map(ReportedAuthorProjection::getUserId)
                .collect(Collectors.toList());
        Map<String, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));

        List<ReportedAuthorResponse> authors = authorPage.getContent().stream()
                .map(p -> toAuthorResponse(p, usersById.get(p.getUserId())))
                .collect(Collectors.toList());

        return PageResponse.<ReportedAuthorResponse>builder()
                .page(safePage)
                .size(authorPage.getSize())
                .totalPages(authorPage.getTotalPages())
                .totalElements(authorPage.getTotalElements())
                .data(authors)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingReportResponse> getApprovedReports(String userId) {
        List<ListingReport> reports =
                listingReportRepository.findByAuthorIdAndStatus(userId, ReportStatus.RESOLVED);
        return reports.stream()
                .map(this::mapToResponseWithAdminInfo)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = Constants.CacheNames.USER_DETAILS, key = "#userId"),
            @CacheEvict(cacheNames = Constants.CacheNames.USER_DETAILS, allEntries = true)
    })
    public ReportedAuthorResponse setPostingBlock(String userId, PostingBlockRequest request, String adminId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        // Single combined query instead of one per status (faster toggle)
        ReportedAuthorProjection.Counts counts = listingReportRepository.getReportCountsByAuthor(userId);
        long totalReports = counts != null && counts.getTotalReports() != null ? counts.getTotalReports() : 0L;
        long resolvedReports = counts != null && counts.getResolvedReports() != null ? counts.getResolvedReports() : 0L;

        boolean block = Boolean.TRUE.equals(request.getBlocked());
        if (block) {
            if (resolvedReports <= BLOCK_ELIGIBLE_THRESHOLD) {
                throw new UserNotBlockEligibleException(BLOCK_ELIGIBLE_THRESHOLD, resolvedReports);
            }
            user.setPostingBlocked(true);
            user.setPostingBlockedReason(request.getReason());
            user.setPostingBlockedByAdminId(adminId);
            user.setPostingBlockedAt(LocalDateTime.now());
            userRepository.save(user);
            log.info("Admin {} BLOCKED user {} from posting. Reason: {}", adminId, userId, request.getReason());

            notificationService.sendNotification(
                    userId, RecipientType.USER,
                    NotificationType.POSTING_BLOCKED,
                    "Tài khoản bị chặn đăng tin",
                    "Tài khoản của bạn đã bị chặn đăng tin do có nhiều tin đăng vi phạm bị báo cáo và đã được duyệt."
                            + (request.getReason() != null && !request.getReason().isBlank()
                                    ? " Lý do: " + request.getReason() : ""),
                    null, "USER"
            );
        } else {
            user.setPostingBlocked(false);
            user.setPostingBlockedReason(null);
            user.setPostingBlockedByAdminId(adminId);
            user.setPostingBlockedAt(null);
            userRepository.save(user);
            log.info("Admin {} UNBLOCKED user {} for posting", adminId, userId);

            notificationService.sendNotification(
                    userId, RecipientType.USER,
                    NotificationType.POSTING_UNBLOCKED,
                    "Tài khoản được mở lại quyền đăng tin",
                    "Tài khoản của bạn đã được mở lại quyền đăng tin. Bạn có thể tiếp tục đăng tin trên SmartRent.",
                    null, "USER"
            );
        }

        return buildResponse(user, totalReports, resolvedReports);
    }

    // ────────────────────────────── helpers ──────────────────────────────

    private ReportedAuthorResponse toAuthorResponse(ReportedAuthorProjection projection, User user) {
        long total = projection.getTotalReports() != null ? projection.getTotalReports() : 0L;
        long resolved = projection.getResolvedReports() != null ? projection.getResolvedReports() : 0L;
        if (user == null) {
            // Author account no longer exists — still surface the counts
            return ReportedAuthorResponse.builder()
                    .userId(projection.getUserId())
                    .totalReports(total)
                    .resolvedReports(resolved)
                    .blockEligible(resolved > BLOCK_ELIGIBLE_THRESHOLD)
                    .postingBlocked(false)
                    .build();
        }
        return buildResponse(user, total, resolved);
    }

    private ReportedAuthorResponse buildResponse(User user, long total, long resolved) {
        return ReportedAuthorResponse.builder()
                .userId(user.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getContactPhoneNumber() != null ? user.getContactPhoneNumber() : user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .totalReports(total)
                .resolvedReports(resolved)
                .blockEligible(resolved > BLOCK_ELIGIBLE_THRESHOLD)
                .postingBlocked(user.isPostingBlocked())
                .postingBlockedReason(user.getPostingBlockedReason())
                .postingBlockedAt(user.getPostingBlockedAt())
                .build();
    }

    private ListingReportResponse mapToResponseWithAdminInfo(ListingReport report) {
        ListingReportResponse response = listingReportMapper.mapFromListingReportEntityToResponse(report);
        if (report.getResolvedBy() != null) {
            adminRepository.findById(report.getResolvedBy()).ifPresent(admin ->
                    response.setResolvedByName(admin.getFirstName() + " " + admin.getLastName()));
        }
        return response;
    }
}
