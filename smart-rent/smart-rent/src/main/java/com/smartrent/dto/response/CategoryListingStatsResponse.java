package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Response DTO for category listing statistics
 * Used in home screen to display listing counts by category
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Thống kê số lượng bài đăng theo category")
public class CategoryListingStatsResponse {

    @Schema(description = "Category ID", example = "1")
    Long categoryId;

    @Schema(description = "Tên category", example = "Căn hộ/Chung cư")
    String categoryName;

    @Schema(description = "Slug của category", example = "can-ho-chung-cu")
    String categorySlug;

    @Schema(description = "Icon của category", example = "apartment")
    String categoryIcon;

    @Schema(description = "Tổng số bài đăng trong category", example = "1250")
    Long totalListings;

    @Schema(description = "Số bài đăng verified", example = "980")
    Long verifiedListings;

    @Schema(description = "Số bài đăng VIP (SILVER, GOLD, DIAMOND)", example = "345")
    Long vipListings;
}