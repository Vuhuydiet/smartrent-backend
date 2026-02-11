package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Extended listing response for owner with additional owner-specific information
 * Includes transaction details, media, payment info, and other private data
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Listing details for owner with additional owner-specific information")
public class ListingResponseForOwner extends ListingResponse {

    @Schema(description = "How the listing was created", example = "QUOTA", allowableValues = {"QUOTA", "DIRECT_PAYMENT"})
    String postSource;

    @Schema(description = "Transaction ID if created via payment", example = "550e8400-e29b-41d4-a716-446655440000")
    String transactionId;

    @Schema(description = "Whether this is a shadow listing (created automatically for DIAMOND tier)", example = "false")
    Boolean isShadow;

    @Schema(description = "Parent listing ID if this is a shadow listing", example = "123")
    Long parentListingId;

    @Schema(description = "Duration in days for this listing", example = "30")
    Integer durationDays;

    @Schema(description = "Whether membership quota was used for this listing", example = "false")
    Boolean useMembershipQuota;

    @Schema(description = "Payment provider used", example = "VNPAY")
    String paymentProvider;

    @Schema(description = "Amount paid for this listing (VND)", example = "1800000")
    BigDecimal amountPaid;

    @Schema(description = "Media (images/videos) attached to this listing")
    List<MediaResponse> media;

    @Schema(description = "Full address information")
    AddressResponse address;

    @Schema(description = "Payment information if created via payment")
    PaymentInfo paymentInfo;

    @Schema(description = "Statistics for this listing (views, contacts, etc.)")
    ListingStatistics statistics;

    @Schema(description = "Verification notes from admin (if any)")
    String verificationNotes;

    @Schema(description = "Reason for rejection (if rejected)")
    String rejectionReason;

    // ── Moderation context ──
    @Schema(description = "Canonical moderation status", example = "REVISION_REQUIRED",
            allowableValues = {"PENDING_REVIEW", "APPROVED", "REJECTED", "REVISION_REQUIRED", "RESUBMITTED", "SUSPENDED"})
    String moderationStatus;

    @Schema(description = "Pending owner action (if any)")
    OwnerActionResponse pendingOwnerAction;

    @Schema(description = "Moderation audit trail (most recent first)")
    List<ModerationEventResponse> moderationTimeline;

    /**
     * Payment information for the listing
     */
    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Schema(description = "Payment information")
    public static class PaymentInfo {
        @Schema(description = "Payment provider", example = "VNPAY")
        String provider;

        @Schema(description = "Payment status", example = "SUCCESS")
        String status;

        @Schema(description = "Payment date")
        LocalDateTime paidAt;

        @Schema(description = "Amount paid (VND)", example = "1800000")
        BigDecimal amount;

        @Schema(description = "VIP tier purchased", example = "GOLD")
        String vipTierPurchased;

        @Schema(description = "Duration purchased (days)", example = "30")
        Integer durationPurchased;
    }

    /**
     * Listing statistics (views, contacts, etc.)
     * These can be implemented later with analytics
     */
    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Schema(description = "Listing statistics")
    public static class ListingStatistics {
        @Schema(description = "Total number of views", example = "1245")
        Long viewCount;

        @Schema(description = "Number of times contact button was clicked", example = "23")
        Long contactCount;

        @Schema(description = "Number of times listing was saved/favorited", example = "15")
        Long saveCount;

        @Schema(description = "Number of reports on this listing", example = "0")
        Long reportCount;

        @Schema(description = "Last viewed at")
        LocalDateTime lastViewedAt;
    }
}