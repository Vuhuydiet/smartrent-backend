package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Response DTO for province listing statistics
 * Used in home screen to display listing counts by province
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Thống kê số lượng bài đăng theo tỉnh/thành phố")
public class ProvinceListingStatsResponse {

    @Schema(description = "Province ID (old structure - 63 provinces)", example = "1")
    Integer provinceId;

    @Schema(description = "Province code (new structure - 34 provinces)", example = "01")
    String provinceCode;

    @Schema(description = "Tên tỉnh/thành phố", example = "Hà Nội")
    String provinceName;

    @Schema(description = "Tổng số bài đăng trong tỉnh/thành phố", example = "1250")
    Long totalListings;

    @Schema(description = "Số bài đăng verified", example = "980")
    Long verifiedListings;

    @Schema(description = "Số bài đăng VIP (SILVER, GOLD, DIAMOND)", example = "345")
    Long vipListings;
}
