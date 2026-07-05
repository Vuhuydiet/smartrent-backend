package com.smartrent.service.broker.impl;

import com.smartrent.config.Constants;
import com.smartrent.dto.request.BrokerRegisterRequest;
import com.smartrent.dto.request.BrokerVerificationRequest;
import com.smartrent.dto.response.AdminBrokerUserResponse;
import com.smartrent.dto.response.BrokerStatusResponse;
import com.smartrent.dto.response.PageResponse;
import com.smartrent.enums.BrokerVerificationStatus;
import com.smartrent.enums.NotificationType;
import com.smartrent.enums.RecipientType;
import com.smartrent.infra.exception.AppException;
import com.smartrent.infra.exception.DomainException;
import com.smartrent.infra.exception.UserNotFoundException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.MediaRepository;
import com.smartrent.infra.repository.UserRepository;
import com.smartrent.infra.repository.entity.Media;
import com.smartrent.infra.repository.entity.User;
import com.smartrent.infra.storage.R2StorageService;
import com.smartrent.service.broker.BrokerService;
import com.smartrent.service.notification.NotificationService;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BrokerServiceImpl implements BrokerService {

    static final String BROKER_VERIFICATION_SOURCE =
            "https://www.nangluchdxd.gov.vn/Canhan?page=2&pagesize=20";

    UserRepository userRepository;
    MediaRepository mediaRepository;
    R2StorageService storageService;
    NotificationService notificationService;

    // ──────────────────────────────────────────────────────────────────
    // User-facing operations
    // ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = Constants.CacheNames.USER_DETAILS, key = "#userId"),
            @CacheEvict(cacheNames = Constants.CacheNames.USER_DETAILS, allEntries = true),
            @CacheEvict(cacheNames = Constants.CacheNames.LISTING_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = Constants.CacheNames.LISTING_BROWSE, allEntries = true),
            @CacheEvict(cacheNames = Constants.CacheNames.LISTING_DETAIL, allEntries = true),
            @CacheEvict(cacheNames = Constants.CacheNames.LISTING_STATS_CATEGORIES, allEntries = true)
    })
    public BrokerStatusResponse registerBroker(String userId, BrokerRegisterRequest request) {
        log.info("Broker registration request for user={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        BrokerVerificationStatus current = user.getBrokerVerificationStatus();

        // Idempotent: already approved or pending – return current state without modification
        if (current == BrokerVerificationStatus.APPROVED) {
            log.info("User {} is already an approved broker – returning current status", userId);
            return toStatusResponse(user);
        }
        if (current == BrokerVerificationStatus.PENDING) {
            log.info("User {} already has a pending broker registration – returning current status", userId);
            return toStatusResponse(user);
        }

        // Validate all three document images (must exist, be owned by user, and be confirmed)
        validateBrokerDocument(userId, request.getCccdFrontMediaId(), "CCCD front");
        validateBrokerDocument(userId, request.getCccdBackMediaId(), "CCCD back");
        validateBrokerDocument(userId, request.getCertMediaId(), "Certificate");

        // NONE or REJECTED → transition to PENDING
        user.setBroker(false);
        user.setBrokerVerificationStatus(BrokerVerificationStatus.PENDING);
        if (user.getBrokerRegisteredAt() == null) {
            user.setBrokerRegisteredAt(LocalDateTime.now());
        }
        // Store document references
        user.setBrokerCccdFrontMediaId(request.getCccdFrontMediaId());
        user.setBrokerCccdBackMediaId(request.getCccdBackMediaId());
        user.setBrokerCertFrontMediaId(request.getCertMediaId());
        user.setBrokerCertBackMediaId(null);
        // Clear previous rejection data on re-registration
        user.setBrokerRejectionReason(null);
        user.setBrokerVerifiedAt(null);
        user.setBrokerVerifiedByAdminId(null);
        user.setBrokerVerificationSource(null);

        userRepository.save(user);
        log.info("Broker registration submitted for user={}, status=PENDING", userId);

        // Notify all admins about the new broker registration
        String fullName = user.getFirstName() + " " + user.getLastName();
        notificationService.sendToAllAdmins(
                NotificationType.BROKER_REGISTRATION_RECEIVED,
                "Yêu cầu đăng ký môi giới mới",
                "Người dùng " + fullName + " đã gửi yêu cầu đăng ký làm môi giới kèm hồ sơ. Vui lòng xem xét và xác minh.",
                null,
                "USER"
        );

        return toStatusResponse(user);
    }

    @Override
    public BrokerStatusResponse getBrokerStatus(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        return toStatusResponse(user);
    }

    // ──────────────────────────────────────────────────────────────────
    // Admin-facing operations
    // ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = Constants.CacheNames.USER_DETAILS, key = "#userId"),
            @CacheEvict(cacheNames = Constants.CacheNames.USER_DETAILS, allEntries = true),
            @CacheEvict(cacheNames = Constants.CacheNames.LISTING_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = Constants.CacheNames.LISTING_BROWSE, allEntries = true),
            @CacheEvict(cacheNames = Constants.CacheNames.LISTING_DETAIL, allEntries = true),
            @CacheEvict(cacheNames = Constants.CacheNames.LISTING_STATS_CATEGORIES, allEntries = true)
    })
    public BrokerStatusResponse reviewBroker(String userId, String adminId, BrokerVerificationRequest request) {
        String action = request.getAction();
        log.info("Admin={} performing broker action={} on user={}", adminId, action, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        if ("APPROVE".equalsIgnoreCase(action)) {
            user.setBroker(true);
            user.setBrokerVerificationStatus(BrokerVerificationStatus.APPROVED);
            user.setBrokerVerifiedAt(LocalDateTime.now());
            user.setBrokerVerifiedByAdminId(adminId);
            user.setBrokerRejectionReason(null);
            user.setBrokerVerificationSource(BROKER_VERIFICATION_SOURCE);

            userRepository.save(user);
            log.info("Broker APPROVED for user={} by admin={}", userId, adminId);

            notificationService.sendNotification(
                    userId, RecipientType.USER,
                    NotificationType.BROKER_APPROVED,
                    "Đăng ký môi giới được chấp thuận",
                    "Chúc mừng! Đăng ký môi giới của bạn đã được xác nhận. Bạn giờ là môi giới được xác nhận trên SmartRent.",
                    null, "USER"
            );

        } else if ("REJECT".equalsIgnoreCase(action)) {
            if (request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
                throw new DomainException(DomainCode.BROKER_REJECTION_REASON_REQUIRED);
            }

            user.setBroker(false);
            user.setBrokerVerificationStatus(BrokerVerificationStatus.REJECTED);
            user.setBrokerVerifiedAt(LocalDateTime.now());
            user.setBrokerVerifiedByAdminId(adminId);
            user.setBrokerRejectionReason(request.getRejectionReason());
            user.setBrokerVerificationSource(BROKER_VERIFICATION_SOURCE);

            userRepository.save(user);
            log.info("Broker REJECTED for user={} by admin={}, reason={}",
                    userId, adminId, request.getRejectionReason());

            notificationService.sendNotification(
                    userId, RecipientType.USER,
                    NotificationType.BROKER_REJECTED,
                    "Đăng ký môi giới bị từ chối",
                    "Đăng ký môi giới của bạn đã bị từ chối. Lý do: " + request.getRejectionReason()
                            + " Bạn có thể nộp lại đơn sau khi đã bổ sung thông tin.",
                    null, "USER"
            );

        } else {
            throw new DomainException(DomainCode.BROKER_INVALID_ACTION, action);
        }

        return toStatusResponse(user);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = Constants.CacheNames.USER_DETAILS, key = "#userId"),
            @CacheEvict(cacheNames = Constants.CacheNames.USER_DETAILS, allEntries = true),
            @CacheEvict(cacheNames = Constants.CacheNames.LISTING_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = Constants.CacheNames.LISTING_BROWSE, allEntries = true),
            @CacheEvict(cacheNames = Constants.CacheNames.LISTING_DETAIL, allEntries = true),
            @CacheEvict(cacheNames = Constants.CacheNames.LISTING_STATS_CATEGORIES, allEntries = true)
    })
    public BrokerStatusResponse removeBrokerRole(String userId, String adminId) {
        log.info("Admin={} removing broker role from user={}", adminId, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        if (!user.isBroker() && user.getBrokerVerificationStatus() == BrokerVerificationStatus.REJECTED) {
            log.info("User {} is already non-broker with REJECTED status; returning current state", userId);
            return toStatusResponse(user);
        }

        user.setBroker(false);
        user.setBrokerVerificationStatus(BrokerVerificationStatus.REJECTED);
        user.setBrokerVerifiedAt(LocalDateTime.now());
        user.setBrokerVerifiedByAdminId(adminId);
        user.setBrokerRejectionReason("Broker role removed by admin");
        user.setBrokerVerificationSource(BROKER_VERIFICATION_SOURCE);

        userRepository.save(user);
        log.info("Broker role removed for user={} by admin={}", userId, adminId);

        notificationService.sendNotification(
                userId, RecipientType.USER,
                NotificationType.BROKER_REJECTED,
                "Quyền môi giới đã bị gỡ",
                "Quyền môi giới của bạn đã bị quản trị viên thu hồi. Bạn có thể đăng ký lại sau khi cập nhật hồ sơ.",
                null, "USER"
        );

        return toStatusResponse(user);
    }

    private static final Set<String> BROKER_PENDING_SORTABLE_FIELDS = Set.of("brokerRegisteredAt", "firstName", "lastName");

    @Override
    public PageResponse<AdminBrokerUserResponse> getPendingBrokers(int page, int size, String search, String registeredAt, String sort) {
        Sort sortObj = buildSort(sort, BROKER_PENDING_SORTABLE_FIELDS, "brokerRegisteredAt", Sort.Direction.ASC);
        Pageable pageable = PageRequest.of(page - 1, size, sortObj);

        Specification<User> spec = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("brokerVerificationStatus"), BrokerVerificationStatus.PENDING);

        if (search != null && !search.isBlank()) {
            String keyword = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), keyword),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), keyword),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), keyword)
            ));
        }

        DateRange range = parseDateOrRange(registeredAt);
        if (range != null) {
            DateRange finalRange = range;
            spec = spec.and((root, query, criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<>();
                if (finalRange.from() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("brokerRegisteredAt"), finalRange.from()));
                }
                if (finalRange.to() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("brokerRegisteredAt"), finalRange.to()));
                }
                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            });
        }

        Page<User> pageResult = userRepository.findAll(spec, pageable);

        List<AdminBrokerUserResponse> items = pageResult.getContent().stream()
                .map(this::toAdminBrokerResponse)
                .toList();

        return PageResponse.<AdminBrokerUserResponse>builder()
                .page(page)
                .size(size)
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .data(items)
                .build();
    }

    private Sort buildSort(String sortParam, Set<String> allowedFields, String defaultField, Sort.Direction defaultDirection) {
        String field = defaultField;
        Sort.Direction direction = defaultDirection;
        if (sortParam != null && !sortParam.isBlank()) {
            String[] parts = sortParam.split(",", 2);
            if (allowedFields.contains(parts[0].trim())) {
                field = parts[0].trim();
            }
            if (parts.length > 1) {
                direction = "ASC".equalsIgnoreCase(parts[1].trim()) ? Sort.Direction.ASC : Sort.Direction.DESC;
            }
        }
        return Sort.by(direction, field);
    }

    /**
     * Parses a single date ("2026-02-09") or a "from..to" range. Either side of a
     * range may be omitted for an open-ended bound. Returns {@code null} when the
     * input is blank.
     */
    private DateRange parseDateOrRange(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return null;
        }
        int idx = s.indexOf("..");
        if (idx < 0) {
            LocalDate date = LocalDate.parse(s);
            return new DateRange(date.atStartOfDay(), date.atTime(LocalTime.MAX));
        }
        String fromStr = s.substring(0, idx).trim();
        String toStr = s.substring(idx + 2).trim();
        LocalDateTime from = fromStr.isEmpty() ? null : LocalDate.parse(fromStr).atStartOfDay();
        LocalDateTime to = toStr.isEmpty() ? null : LocalDate.parse(toStr).atTime(LocalTime.MAX);
        if (from == null && to == null) {
            return null;
        }
        return new DateRange(from, to);
    }

    private record DateRange(LocalDateTime from, LocalDateTime to) {}

    // ──────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────

    /**
     * Validates a broker document media record:
     * - Must exist
     * - Must be owned by the user
     * - Must be ACTIVE (upload confirmed)
     * - Must be an image
     */
    private void validateBrokerDocument(String userId, Long mediaId, String docName) {
        if (mediaId == null) {
            throw new DomainException(DomainCode.BROKER_DOCUMENT_REQUIRED, docName);
        }
        Media media = mediaRepository.findByMediaIdAndUserId(mediaId, userId)
                .orElseThrow(() -> new DomainException(DomainCode.BROKER_DOCUMENT_NOT_FOUND, docName));
        if (media.getStatus() != Media.MediaStatus.ACTIVE || !Boolean.TRUE.equals(media.getUploadConfirmed())) {
            throw new DomainException(DomainCode.BROKER_DOCUMENT_NOT_CONFIRMED, docName);
        }
        if (media.getMediaType() != Media.MediaType.IMAGE) {
            throw new DomainException(DomainCode.BROKER_DOCUMENT_INVALID_TYPE, docName);
        }
    }

    /**
     * Generates a short-lived presigned download URL for a broker document.
     * Returns null if the media ID is null or the record is unavailable.
     */
    private String resolveDocumentUrl(Long mediaId) {
        if (mediaId == null) return null;
        return mediaRepository.findById(mediaId)
                .filter(m -> m.getStatus() == Media.MediaStatus.ACTIVE)
                .filter(m -> m.getStorageKey() != null)
                .map(m -> storageService.generateDownloadUrl(m.getStorageKey()).getUrl())
                .orElse(null);
    }

    /**
     * Uses the current single-certificate field and falls back to historical back-side data.
     */
    private Long resolveCertificateMediaId(User user) {
        if (user.getBrokerCertFrontMediaId() != null) {
            return user.getBrokerCertFrontMediaId();
        }
        return user.getBrokerCertBackMediaId();
    }

    private BrokerStatusResponse toStatusResponse(User user) {
        return BrokerStatusResponse.builder()
                .userId(user.getUserId())
                .isBroker(user.isBroker())
                .brokerVerificationStatus(
                        user.getBrokerVerificationStatus() != null
                                ? user.getBrokerVerificationStatus().name()
                                : BrokerVerificationStatus.NONE.name())
                .brokerRegisteredAt(user.getBrokerRegisteredAt())
                .brokerVerifiedAt(user.getBrokerVerifiedAt())
                .brokerRejectionReason(user.getBrokerRejectionReason())
                .brokerVerificationSource(user.getBrokerVerificationSource())
                // Document viewing URLs (presigned, generated on demand)
                .cccdFrontUrl(resolveDocumentUrl(user.getBrokerCccdFrontMediaId()))
                .cccdBackUrl(resolveDocumentUrl(user.getBrokerCccdBackMediaId()))
                .certUrl(resolveDocumentUrl(resolveCertificateMediaId(user)))
                .build();
    }

    private AdminBrokerUserResponse toAdminBrokerResponse(User user) {
        return AdminBrokerUserResponse.builder()
                .userId(user.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneCode(user.getPhoneCode())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .brokerVerificationStatus(
                        user.getBrokerVerificationStatus() != null
                                ? user.getBrokerVerificationStatus().name()
                                : BrokerVerificationStatus.NONE.name())
                .brokerRegisteredAt(user.getBrokerRegisteredAt())
                .brokerRejectionReason(user.getBrokerRejectionReason())
                // Document viewing URLs (presigned, generated on demand for admin)
                .cccdFrontUrl(resolveDocumentUrl(user.getBrokerCccdFrontMediaId()))
                .cccdBackUrl(resolveDocumentUrl(user.getBrokerCccdBackMediaId()))
                .certUrl(resolveDocumentUrl(resolveCertificateMediaId(user)))
                .build();
    }
}
