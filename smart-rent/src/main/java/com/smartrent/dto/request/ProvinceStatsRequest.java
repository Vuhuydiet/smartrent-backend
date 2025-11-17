package com.smartrent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Request DTO for fetching province listing statistics
 * PUBLIC API - NO userId or authentication required
 * Allows frontend to request stats for multiple provinces at once
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request để lấy thống kê bài đăng theo danh sách tỉnh/thành phố. ⚠️ KHÔNG CẦN userId - API công khai")
public class ProvinceStatsRequest {

    @Schema(
        description = "Danh sách Province ID (old structure - 63 provinces). Chỉ cần truyền provinceIds HOẶC provinceCodes, không cần cả hai.",
        example = "[1, 48, 79]",
        nullable = true
    )
    List<Integer> provinceIds;

    @Schema(
        description = "Danh sách Province Code (new structure - 34 provinces). Chỉ cần truyền provinceIds HOẶC provinceCodes, không cần cả hai.",
        example = "[\"01\", \"48\", \"79\"]",
        nullable = true
    )
    List<String> provinceCodes;

    @Schema(
        description = "Chỉ đếm bài đăng verified (mặc định: false - đếm tất cả)",
        example = "false",
        defaultValue = "false"
    )
    @Builder.Default
    Boolean verifiedOnly = false;

    @Schema(
        description = "Loại address structure: OLD (63 tỉnh) hoặc NEW (34 tỉnh). Nếu không truyền, tự động detect từ provinceIds/provinceCodes",
        example = "OLD",
        allowableValues = {"OLD", "NEW"}
    )
    String addressType;
}
