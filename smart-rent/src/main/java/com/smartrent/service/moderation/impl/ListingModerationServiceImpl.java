package com.smartrent.service.moderation.impl;

import com.smartrent.dto.request.ListingStatusChangeRequest;
import com.smartrent.dto.request.ResolveReportRequest;
import com.smartrent.dto.request.ResubmitListingRequest;
import com.smartrent.dto.response.ListingResponseWithAdmin;
import com.smartrent.dto.response.ModerationEventResponse;
import com.smartrent.dto.response.OwnerActionResponse;
import com.smartrent.dto.response.UserCreationResponse;
import com.smartrent.enums.*;
import com.smartrent.infra.exception.DomainException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.AdminRepository;
import com.smartrent.infra.repository.ListingModerationEventRepository;
import com.smartrent.infra.repository.ListingOwnerActionRepository;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.UserRepository;
import com.smartrent.infra.repository.entity.Admin;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.ListingModerationEvent;
import com.smartrent.infra.repository.entity.ListingOwnerAction;
import com.smartrent.mapper.ListingMapper;
import com.smartrent.mapper.UserMapper;
import com.smartrent.service.moderation.ListingModerationService;
import com.smartrent.utility.ModerationEmailBuilder;
import com.smartrent.infra.connector.model.EmailInfo;
import com.smartrent.infra.connector.model.EmailRequest;
import com.smartrent.service.email.EmailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ListingModerationServiceImpl implements ListingModerationService {

    ListingRepository listingRepository;
    ListingModerationEventRepository moderationEventRepository;
    ListingOwnerActionRepository ownerActionRepository;
    AdminRepository adminRepository;
    UserRepository userRepository;
    ListingMapper listingMapper;
    UserMapper userMapper;
    EmailService emailService;

    // ───────────────────────────────────────────────────────────────
    // Admin moderation decision
    // ───────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ListingResponseWithAdmin moderateListing(Long listingId, ListingStatusChangeRequest request, String adminId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new DomainException(DomainCode.LISTING_NOT_FOUND));

        // Resolve decision (support both old and new request format)
        String decision = resolveDecision(request);
        ModerationStatus previousStatus = listing.getModerationStatus();

        switch (decision) {
            case "APPROVE" -> handleApprove(listing, adminId);
            case "REJECT" -> handleRejectOrRevision(listing, request, adminId, ModerationStatus.REJECTED, ModerationAction.REJECT);
            case "REQUEST_REVISION" -> handleRejectOrRevision(listing, request, adminId, ModerationStatus.REVISION_REQUIRED, ModerationAction.REQUEST_REVISION);
            default -> throw new DomainException(DomainCode.MODERATION_INVALID_DECISION);
        }

        Listing saved = listingRepository.save(listing);

        // Audit event
        createModerationEvent(saved.getListingId(), determineModerationSource(previousStatus),
                previousStatus, saved.getModerationStatus(),
                ModerationAction.valueOf(decision), adminId, null,
                resolveReasonCode(request), resolveReasonText(request), null);

        // Async email notification (never fail the transaction)
        sendModerationEmailAsync(saved, decision, resolveReasonText(request));

        // Build response
        Admin admin = adminRepository.findById(adminId).orElse(null);
        var userEntity = userRepository.findById(saved.getUserId()).orElse(null);
        UserCreationResponse user = userEntity != null ? userMapper.mapFromUserEntityToUserCreationResponse(userEntity) : null;
        String verificationStatus = mapToVerificationStatus(saved);
        String verificationNotes = resolveReasonText(request);

        ListingResponseWithAdmin response = listingMapper.toResponseWithAdmin(saved, user, admin, verificationStatus, verificationNotes);
        // Set moderation context on response
        response.setModerationStatus(saved.getModerationStatus() != null ? saved.getModerationStatus().name() : null);
        response.setRevisionCount(saved.getRevisionCount());
        response.setLastModerationReasonCode(saved.getLastModerationReasonCode());
        response.setLastModerationReasonText(saved.getLastModerationReasonText());
        return response;
    }

    // ───────────────────────────────────────────────────────────────
    // Report resolution with owner action
    // ───────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void handleReportResolutionOwnerAction(Long reportId, Long listingId, ResolveReportRequest request, String adminId) {
        if (!Boolean.TRUE.equals(request.getOwnerActionRequired())) {
            return; // No owner action needed
        }

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new DomainException(DomainCode.LISTING_NOT_FOUND));

        ModerationStatus previousStatus = listing.getModerationStatus();

        // Hide listing if requested
        if ("HIDE_UNTIL_REVIEW".equalsIgnoreCase(request.getListingVisibilityAction())) {
            listing.setModerationStatus(ModerationStatus.REVISION_REQUIRED);
            listing.setVerified(false);
            listing.setIsVerify(false);
            listing.setLastModeratedBy(adminId);
            listing.setLastModeratedAt(LocalDateTime.now());
            listing.setLastModerationReasonText(request.getAdminNotes());
            listingRepository.save(listing);
        }

        // Create owner action
        OwnerActionType actionType = OwnerActionType.UPDATE_LISTING;
        if (request.getOwnerActionType() != null) {
            try {
                actionType = OwnerActionType.valueOf(request.getOwnerActionType());
            } catch (IllegalArgumentException ignored) {
                // default to UPDATE_LISTING
            }
        }

        ListingOwnerAction ownerAction = ListingOwnerAction.builder()
                .listingId(listingId)
                .triggerType(OwnerActionTriggerType.REPORT_RESOLVED)
                .triggerRefId(reportId)
                .requiredAction(actionType)
                .status(OwnerActionStatus.PENDING_OWNER)
                .deadlineAt(request.getOwnerActionDeadlineAt())
                .build();
        ownerActionRepository.save(ownerAction);

        // Audit event
        createModerationEvent(listingId, ModerationSource.REPORT_RESOLUTION,
                previousStatus,
                "HIDE_UNTIL_REVIEW".equalsIgnoreCase(request.getListingVisibilityAction())
                        ? ModerationStatus.REVISION_REQUIRED : previousStatus,
                ModerationAction.REQUEST_REVISION, adminId, null,
                null, request.getAdminNotes(), reportId);

        // Async email
        sendReportActionEmailAsync(listing, request.getAdminNotes());

        log.info("Created owner action for listing {} from report {}", listingId, reportId);
    }

    // ───────────────────────────────────────────────────────────────
    // Owner resubmit
    // ───────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void resubmitForReview(Long listingId, String userId, ResubmitListingRequest request) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new DomainException(DomainCode.LISTING_NOT_FOUND));

        // Validate ownership
        if (!listing.getUserId().equals(userId)) {
            throw new DomainException(DomainCode.NOT_LISTING_OWNER);
        }

        // Validate state: must be REJECTED, REVISION_REQUIRED, or have pending owner action
        boolean canResubmit = false;
        if (listing.getModerationStatus() != null) {
            canResubmit = listing.getModerationStatus() == ModerationStatus.REJECTED
                    || listing.getModerationStatus() == ModerationStatus.REVISION_REQUIRED;
        }
        // Also allow if legacy REJECTED state (verified=false, isVerify=false)
        if (!canResubmit && Boolean.FALSE.equals(listing.getVerified()) && Boolean.FALSE.equals(listing.getIsVerify())) {
            canResubmit = true;
        }
        if (!canResubmit) {
            throw new DomainException(DomainCode.RESUBMIT_NOT_ALLOWED);
        }

        ModerationStatus previousStatus = listing.getModerationStatus();

        // Transition listing state
        listing.setModerationStatus(ModerationStatus.PENDING_REVIEW);
        listing.setVerified(false);
        listing.setIsVerify(true); // Back to IN_REVIEW in legacy view
        listing.setRevisionCount(listing.getRevisionCount() + 1);
        listingRepository.save(listing);

        // Advance any pending owner actions
        List<ListingOwnerAction> pendingActions = ownerActionRepository
                .findByListingIdAndStatus(listingId, OwnerActionStatus.PENDING_OWNER);
        for (ListingOwnerAction action : pendingActions) {
            action.setStatus(OwnerActionStatus.SUBMITTED_FOR_REVIEW);
            action.setCompletedAt(LocalDateTime.now());
        }
        ownerActionRepository.saveAll(pendingActions);

        // Audit event
        createModerationEvent(listingId, ModerationSource.OWNER_EDIT,
                previousStatus, ModerationStatus.PENDING_REVIEW,
                ModerationAction.RESUBMIT, null, userId,
                null, request != null ? request.getNotes() : null, null);

        // Notify admins asynchronously
        sendResubmitNotificationAsync(listing);

        log.info("Listing {} resubmitted for review by user {}", listingId, userId);
    }

    // ───────────────────────────────────────────────────────────────
    // Read helpers
    // ───────────────────────────────────────────────────────────────
    @Override
    public OwnerActionResponse getOwnerPendingAction(Long listingId) {
        return ownerActionRepository.findByListingIdAndStatus(listingId, OwnerActionStatus.PENDING_OWNER)
                .stream()
                .findFirst()
                .map(this::mapOwnerAction)
                .orElse(null);
    }

    @Override
    public List<ModerationEventResponse> getModerationTimeline(Long listingId) {
        return moderationEventRepository.findByListingIdOrderByCreatedAtDesc(listingId)
                .stream()
                .map(this::mapModerationEvent)
                .collect(Collectors.toList());
    }

    // ───────────────────────────────────────────────────────────────
    // Private helpers
    // ───────────────────────────────────────────────────────────────
    private void handleApprove(Listing listing, String adminId) {
        listing.setModerationStatus(ModerationStatus.APPROVED);
        listing.setVerified(true);
        listing.setIsVerify(true);
        listing.setLastModeratedBy(adminId);
        listing.setLastModeratedAt(LocalDateTime.now());

        // Calculate expiry date
        LocalDateTime approvalTime = LocalDateTime.now();
        LocalDateTime postDate = listing.getPostDate();
        int durationDays = listing.getDurationDays() != null ? listing.getDurationDays() : 30;

        if (postDate != null && approvalTime.isAfter(postDate)) {
            listing.setExpiryDate(approvalTime.plusDays(durationDays));
        } else {
            LocalDateTime baseDate = postDate != null ? postDate : approvalTime;
            listing.setExpiryDate(baseDate.plusDays(durationDays));
        }

        // Complete any pending owner actions
        List<ListingOwnerAction> pendingActions = ownerActionRepository
                .findByListingIdAndStatus(listing.getListingId(), OwnerActionStatus.SUBMITTED_FOR_REVIEW);
        for (ListingOwnerAction action : pendingActions) {
            action.setStatus(OwnerActionStatus.COMPLETED);
            action.setCompletedAt(LocalDateTime.now());
        }
        if (!pendingActions.isEmpty()) {
            ownerActionRepository.saveAll(pendingActions);
        }
    }

    private void handleRejectOrRevision(Listing listing, ListingStatusChangeRequest request,
                                         String adminId, ModerationStatus status, ModerationAction action) {
        // Require reason
        String reasonText = resolveReasonText(request);
        if (reasonText == null || reasonText.isBlank()) {
            throw new DomainException(DomainCode.MODERATION_REASON_REQUIRED);
        }

        listing.setModerationStatus(status);
        listing.setVerified(false);
        listing.setIsVerify(false);
        listing.setLastModeratedBy(adminId);
        listing.setLastModeratedAt(LocalDateTime.now());
        listing.setLastModerationReasonCode(resolveReasonCode(request));
        listing.setLastModerationReasonText(reasonText);

        // Create owner action if requested
        if (Boolean.TRUE.equals(request.getOwnerActionRequired())) {
            ListingOwnerAction ownerAction = ListingOwnerAction.builder()
                    .listingId(listing.getListingId())
                    .triggerType(OwnerActionTriggerType.LISTING_REJECTED)
                    .requiredAction(OwnerActionType.UPDATE_LISTING)
                    .status(OwnerActionStatus.PENDING_OWNER)
                    .deadlineAt(request.getOwnerActionDeadlineAt())
                    .build();
            ownerActionRepository.save(ownerAction);
        }
    }

    private String resolveDecision(ListingStatusChangeRequest request) {
        if (request.isNewFormat()) {
            return request.getDecision().toUpperCase();
        }
        // Legacy adapter
        if (Boolean.TRUE.equals(request.getVerified())) {
            return "APPROVE";
        }
        return "REJECT";
    }

    private String resolveReasonCode(ListingStatusChangeRequest request) {
        return request.isNewFormat() ? request.getReasonCode() : null;
    }

    private String resolveReasonText(ListingStatusChangeRequest request) {
        if (request.isNewFormat()) {
            return request.getReasonText();
        }
        return request.getReason();
    }

    private ModerationSource determineModerationSource(ModerationStatus previousStatus) {
        if (previousStatus == null || previousStatus == ModerationStatus.PENDING_REVIEW) {
            return ModerationSource.NEW_SUBMISSION;
        }
        if (previousStatus == ModerationStatus.RESUBMITTED) {
            return ModerationSource.OWNER_EDIT;
        }
        return ModerationSource.NEW_SUBMISSION;
    }

    private String mapToVerificationStatus(Listing listing) {
        if (Boolean.TRUE.equals(listing.getVerified())) return "APPROVED";
        if (Boolean.TRUE.equals(listing.getIsVerify())) return "PENDING";
        return "REJECTED";
    }

    private void createModerationEvent(Long listingId, ModerationSource source,
                                        ModerationStatus fromStatus, ModerationStatus toStatus,
                                        ModerationAction action, String adminId, String userId,
                                        String reasonCode, String reasonText, Long reportId) {
        ListingModerationEvent event = ListingModerationEvent.builder()
                .listingId(listingId)
                .source(source)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .action(action)
                .reasonCode(reasonCode)
                .reasonText(reasonText)
                .adminId(adminId)
                .triggeredByUserId(userId)
                .reportId(reportId)
                .build();
        moderationEventRepository.save(event);
    }

    // ── Mapper helpers ──
    private ModerationEventResponse mapModerationEvent(ListingModerationEvent event) {
        // Resolve admin name if available
        String adminName = null;
        if (event.getAdminId() != null) {
            adminName = adminRepository.findById(event.getAdminId())
                    .map(Admin::getEmail)
                    .orElse(null);
        }
        return ModerationEventResponse.builder()
                .eventId(event.getEventId())
                .source(event.getSource() != null ? event.getSource().name() : null)
                .fromStatus(event.getFromStatus() != null ? event.getFromStatus().name() : null)
                .toStatus(event.getToStatus() != null ? event.getToStatus().name() : null)
                .action(event.getAction() != null ? event.getAction().name() : null)
                .reasonCode(event.getReasonCode())
                .reasonText(event.getReasonText())
                .adminId(event.getAdminId())
                .adminName(adminName)
                .triggeredByUserId(event.getTriggeredByUserId())
                .reportId(event.getReportId())
                .createdAt(event.getCreatedAt())
                .build();
    }

    private OwnerActionResponse mapOwnerAction(ListingOwnerAction action) {
        return OwnerActionResponse.builder()
                .ownerActionId(action.getOwnerActionId())
                .listingId(action.getListingId())
                .triggerType(action.getTriggerType() != null ? action.getTriggerType().name() : null)
                .triggerRefId(action.getTriggerRefId())
                .requiredAction(action.getRequiredAction() != null ? action.getRequiredAction().name() : null)
                .status(action.getStatus() != null ? action.getStatus().name() : null)
                .deadlineAt(action.getDeadlineAt())
                .completedAt(action.getCompletedAt())
                .createdAt(action.getCreatedAt())
                .build();
    }

    // ── Email helpers (async, never fails transaction) ──
    private void sendModerationEmailAsync(Listing listing, String decision, String reasonText) {
        try {
            var userEntity = userRepository.findById(listing.getUserId()).orElse(null);
            if (userEntity == null || userEntity.getEmail() == null) return;

            String subject;
            String htmlContent;
            switch (decision) {
                case "APPROVE" -> {
                    subject = "Your listing has been approved - SmartRent";
                    htmlContent = ModerationEmailBuilder.buildApprovedEmail(listing.getTitle(), userEntity.getFirstName());
                }
                case "REJECT" -> {
                    subject = "Your listing has been rejected - SmartRent";
                    htmlContent = ModerationEmailBuilder.buildRejectedEmail(listing.getTitle(), userEntity.getFirstName(), reasonText);
                }
                case "REQUEST_REVISION" -> {
                    subject = "Revision required for your listing - SmartRent";
                    htmlContent = ModerationEmailBuilder.buildRevisionRequestedEmail(listing.getTitle(), userEntity.getFirstName(), reasonText);
                }
                default -> { return; }
            }

            EmailRequest emailRequest = EmailRequest.builder()
                    .sender(EmailInfo.builder().name("SmartRent").email("no-reply@smartrent.vn").build())
                    .to(List.of(EmailInfo.builder().name(userEntity.getFirstName()).email(userEntity.getEmail()).build()))
                    .subject(subject)
                    .htmlContent(htmlContent)
                    .build();
            emailService.sendEmail(emailRequest);
        } catch (Exception e) {
            log.warn("Failed to send moderation email for listing {}: {}", listing.getListingId(), e.getMessage());
        }
    }

    private void sendReportActionEmailAsync(Listing listing, String adminNotes) {
        try {
            var userEntity = userRepository.findById(listing.getUserId()).orElse(null);
            if (userEntity == null || userEntity.getEmail() == null) return;

            EmailRequest emailRequest = EmailRequest.builder()
                    .sender(EmailInfo.builder().name("SmartRent").email("no-reply@smartrent.vn").build())
                    .to(List.of(EmailInfo.builder().name(userEntity.getFirstName()).email(userEntity.getEmail()).build()))
                    .subject("Action required for your listing - SmartRent")
                    .htmlContent(ModerationEmailBuilder.buildReportActionRequiredEmail(listing.getTitle(), userEntity.getFirstName(), adminNotes))
                    .build();
            emailService.sendEmail(emailRequest);
        } catch (Exception e) {
            log.warn("Failed to send report action email for listing {}: {}", listing.getListingId(), e.getMessage());
        }
    }

    private void sendResubmitNotificationAsync(Listing listing) {
        try {
            log.info("Listing {} resubmitted — notifying admin review queue", listing.getListingId());
            // In a production system, this would notify the admin queue via email or push.
            // For now, we log it. Can be extended to send to admin email list.
        } catch (Exception e) {
            log.warn("Failed to send resubmit notification for listing {}: {}", listing.getListingId(), e.getMessage());
        }
    }
}
