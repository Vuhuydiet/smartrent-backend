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
import java.util.List;

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

    @Schema(description = "Whether listing is expired")
    Boolean expired;

    @Schema(description = "Computed listing status", example = "IN_REVIEW")
    String listingStatus;

    @Schema(description = "VIP tier", example = "DIAMOND", allowableValues = { "NORMAL", "SILVER", "GOLD", "DIAMOND" })
    String vipType;

    @Schema(description = "Product type", example = "APARTMENT", allowableValues = { "ROOM", "APARTMENT", "HOUSE", "OFFICE", "STUDIO" })
    String productType;

    @Schema(description = "Price", example = "14200000")
    BigDecimal price;

    @Schema(description = "Price unit", example = "MONTH")
    String priceUnit;

    @Schema(description = "Area in m²", example = "34.1")
    Float area;

    @Schema(description = "District name (legacy address structure only; null for new-structure addresses)", example = "Quận Tân Bình")
    String district;

    @Schema(description = "Full formatted address", example = "123 Nguyễn Trãi, Phường 5, Quận Tân Bình, TP. Hồ Chí Minh")
    String fullAddress;

    @Schema(description = "Image URLs (primary first, then sorted by sortOrder)",
            example = "[\"https://cdn.example.com/img1.jpg\", \"https://cdn.example.com/img2.jpg\"]")
    List<String> images;

    @Schema(description = "Admin verification summary (verificationStatus only)")
    AdminVerificationSummary adminVerification;

    @Schema(description = "Canonical moderation status", example = "PENDING_REVIEW", allowableValues = {
            "PENDING_REVIEW", "APPROVED", "REJECTED", "REVISION_REQUIRED", "RESUBMITTED", "SUSPENDED" })
    String moderationStatus;

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

        @Schema(description = "Verification status", example = "PENDING", allowableValues = { "PENDING", "APPROVED", "REJECTED", "NOT_SUBMITTED" })
        String verificationStatus;
    }
}
