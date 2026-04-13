package com.smartrent.service.broker.impl;

import com.smartrent.config.Constants;
import com.smartrent.dto.request.BrokerVerificationRequest;
import com.smartrent.dto.response.AdminBrokerUserResponse;
import com.smartrent.dto.response.BrokerStatusResponse;
import com.smartrent.dto.response.PageResponse;
import com.smartrent.enums.BrokerVerificationStatus;
import com.smartrent.enums.NotificationType;
import com.smartrent.enums.RecipientType;
import com.smartrent.infra.exception.DomainException;
import com.smartrent.infra.exception.UserNotFoundException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.UserRepository;
import com.smartrent.infra.repository.entity.User;
import com.smartrent.service.broker.BrokerService;
import com.smartrent.service.notification.NotificationService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BrokerServiceImpl implements BrokerService {

    static final String BROKER_VERIFICATION_SOURCE =
            "https://www.nangluchdxd.gov.vn/Canhan?page=2&pagesize=20";

    UserRepository userRepository;
    NotificationService notificationService;

    // ──────────────────────────────────────────────────────────────────
    // User-facing operations
    // ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(cacheNames = Constants.CacheNames.USER_DETAILS, key = "#userId")
    public BrokerStatusResponse registerBroker(String userId) {
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

        // NONE or REJECTED → transition to PENDING
        user.setBroker(false);
        user.setBrokerVerificationStatus(BrokerVerificationStatus.PENDING);
        if (user.getBrokerRegisteredAt() == null) {
            user.setBrokerRegisteredAt(LocalDateTime.now());
        }
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
                "Người dùng " + fullName + " đã gửi yêu cầu đăng ký làm môi giới. Vui lòng xem xét và xác minh.",
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
    @CacheEvict(cacheNames = Constants.CacheNames.USER_DETAILS, key = "#userId")
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

            // Notify the user about approval
            notificationService.sendNotification(
                    userId, RecipientType.USER,
                    NotificationType.BROKER_APPROVED,
                    "Đăng ký môi giới được chấp thuận",
                    "Chúc mừng! Đăng ký môi giới của bạn đã được xác nhận. Bạn giờ là môi giới được xác nhận trên SmartRent.",
                    null,
                    "USER"
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

            // Notify the user about rejection
            notificationService.sendNotification(
                    userId, RecipientType.USER,
                    NotificationType.BROKER_REJECTED,
                    "Đăng ký môi giới bị từ chối",
                    "Đăng ký môi giới của bạn đã bị từ chối. Lý do: " + request.getRejectionReason()
                            + " Bạn có thể nộp lại đơn sau khi đã bổ sung thông tin.",
                    null,
                    "USER"
            );

        } else {
            throw new DomainException(DomainCode.BROKER_INVALID_ACTION, action);
        }

        return toStatusResponse(user);
    }

    @Override
    public PageResponse<AdminBrokerUserResponse> getPendingBrokers(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<User> pageResult = userRepository
                .findAllByBrokerVerificationStatusOrderByBrokerRegisteredAtAsc(
                        BrokerVerificationStatus.PENDING, pageable);

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

    // ──────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────

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
                .isBroker(user.isBroker())
                .brokerVerificationStatus(
                        user.getBrokerVerificationStatus() != null
                                ? user.getBrokerVerificationStatus().name()
                                : BrokerVerificationStatus.NONE.name())
                .brokerRegisteredAt(user.getBrokerRegisteredAt())
                .brokerVerifiedAt(user.getBrokerVerifiedAt())
                .brokerVerifiedByAdminId(user.getBrokerVerifiedByAdminId())
                .brokerRejectionReason(user.getBrokerRejectionReason())
                .build();
    }
}
