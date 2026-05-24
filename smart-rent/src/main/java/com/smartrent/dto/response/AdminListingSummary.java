package com.smartrent.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Slim listing summary for admin list/table view. Use GET /v1/listings/admin/{id} for the full record.")
public class AdminListingSummary {

    @Schema(description = "Listing ID", example = "667419")
    Long listingId;

    @Schema(description = "Listing title", example = "Cho thuê căn hộ tầng cao, view thoáng Quận Tân Bình - 34m²")
    String title;

    @Schema(description = "Owner summary (name + contact phone only)")
    OwnerSummary user;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Post date")
    LocalDateTime postDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Expiry date")
    LocalDateTime expiryDate;

    @Schema(description = "Listing type", example = "SHARE", allowableValues = { "RENT", "SALE", "SHARE" })
    String listingType;

    @Schema(description = "Whether listing is verified")
    Boolean verified;

    @Schema(description = "Whether listing is expired")
    Boolean expired;

    @Schema(description = "Computed listing status", example = "IN_REVIEW")
    String listingStatus;

    @Schema(description = "VIP tier", example = "DIAMOND", allowableValues = { "NORMAL", "SILVER", "GOLD", "DIAMOND" })
    String vipType;

    @Schema(description = "Category ID", example = "2")
    Long categoryId;

    @Schema(description = "Product type", example = "APARTMENT", allowableValues = { "ROOM", "APARTMENT", "HOUSE", "OFFICE", "STUDIO" })
    String productType;

    @Schema(description = "Price", example = "14200000")
    BigDecimal price;

    @Schema(description = "Price unit", example = "MONTH")
    String priceUnit;

    @Schema(description = "Area in m²", example = "34.1")
    Float area;

    @Schema(description = "Admin verification summary (verifiedAt + verificationStatus only)")
    AdminVerificationSummary adminVerification;

    @Schema(description = "Canonical moderation status", example = "PENDING_REVIEW", allowableValues = {
            "PENDING_REVIEW", "APPROVED", "REJECTED", "REVISION_REQUIRED", "RESUBMITTED", "SUSPENDED" })
    String moderationStatus;

    @Schema(description = "Number of revisions", example = "0")
    Integer revisionCount;

    @Schema(description = "Structured reason code for last moderation action", example = "MISSING_INFO")
    String lastModerationReasonCode;

    @Schema(description = "Human-readable reason for last moderation action")
    String lastModerationReasonText;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Owner summary for admin list view")
    public static class OwnerSummary {

        @Schema(description = "First name", example = "Trương")
        String firstName;

        @Schema(description = "Last name", example = "Quốc Phú")
        String lastName;

        @Schema(description = "Contact phone number", example = "0367919024")
        String contactPhoneNumber;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Admin verification summary for admin list view")
    public static class AdminVerificationSummary {

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "Verification timestamp", example = "2026-05-24T04:08:12")
        LocalDateTime verifiedAt;

        @Schema(description = "Verification status", example = "PENDING", allowableValues = { "PENDING", "APPROVED", "REJECTED", "NOT_SUBMITTED" })
        String verificationStatus;
    }
}
