package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Response DTO for map-based listing queries
 * Contains listings within the requested map bounds
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Response chứa danh sách bài đăng trong vùng hiển thị trên bản đồ")
public class MapListingsResponse {

    @Schema(
        description = "Danh sách bài đăng trong vùng bản đồ. Đã được sắp xếp theo VIP type (DIAMOND > GOLD > SILVER > NORMAL) và updatedAt (mới nhất trước).",
        example = "[...]"
    )
    List<ListingResponse> listings;

    @Schema(
        description = "Tổng số lượng bài đăng trong vùng (có thể nhiều hơn số lượng trả về do limit)",
        example = "235"
    )
    Long totalCount;

    @Schema(
        description = "Số lượng bài đăng thực tế trả về (đã áp dụng limit)",
        example = "100"
    )
    Integer returnedCount;

    @Schema(
        description = "Có còn nhiều bài đăng hơn không (true nếu totalCount > returnedCount)",
        example = "true"
    )
    Boolean hasMore;

    @Schema(
        description = "Thông tin vùng bản đồ được query",
        implementation = MapBoundsInfo.class
    )
    MapBoundsInfo bounds;

    /**
     * Nested class chứa thông tin về bounds đã query
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Schema(description = "Thông tin về vùng bản đồ")
    public static class MapBoundsInfo {

        @Schema(description = "Vĩ độ góc Đông Bắc", example = "10.823")
        java.math.BigDecimal neLat;

        @Schema(description = "Kinh độ góc Đông Bắc", example = "106.701")
        java.math.BigDecimal neLng;

        @Schema(description = "Vĩ độ góc Tây Nam", example = "10.705")
        java.math.BigDecimal swLat;

        @Schema(description = "Kinh độ góc Tây Nam", example = "106.590")
        java.math.BigDecimal swLng;

        @Schema(description = "Mức zoom", example = "14")
        Integer zoom;
    }
}