package com.smartrent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Filter criteria for my listings")
public class MyListingsFilterRequest {

    @Schema(description = "Filter by verified status", example = "true")
    Boolean verified;

    @Schema(description = "Filter by verification in progress", example = "false")
    Boolean isVerify;

    @Schema(description = "Filter by expired status", example = "false")
    Boolean expired;

    @Schema(description = "Filter by draft status", example = "false")
    Boolean isDraft;

    @Schema(description = "VIP type filter", allowableValues = {"NORMAL", "SILVER", "GOLD", "DIAMOND"})
    String vipType;

    @Schema(description = "Listing type filter", allowableValues = {"RENT", "SALE", "SHARE"})
    String listingType;

    @Schema(description = "Page number (one-based)", example = "1", defaultValue = "1")
    @Builder.Default
    Integer page = 1;

    @Schema(description = "Page size", example = "20", defaultValue = "20")
    @Builder.Default
    Integer size = 20;

    @Schema(description = "Sort by field", example = "createdAt", allowableValues = {"createdAt", "postDate", "price", "updatedAt"})
    @Builder.Default
    String sortBy = "createdAt";

    @Schema(description = "Sort direction", example = "DESC", allowableValues = {"ASC", "DESC"})
    @Builder.Default
    String sortDirection = "DESC";
}
