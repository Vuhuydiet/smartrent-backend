package com.smartrent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Request DTO for fetching category listing statistics
 * PUBLIC API - NO userId or authentication required
 * Allows frontend to request stats for multiple categories at once
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request để lấy thống kê bài đăng theo danh sách categories. ⚠️ KHÔNG CẦN userId - API công khai")
public class CategoryStatsRequest {

    @Schema(
        description = "Danh sách Category ID",
        example = "[1, 2, 3, 4, 5]",
        nullable = false,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<Long> categoryIds;

    @Schema(
        description = "Chỉ đếm bài đăng verified (mặc định: false - đếm tất cả)",
        example = "false",
        defaultValue = "false"
    )
    @Builder.Default
    Boolean verifiedOnly = false;
}