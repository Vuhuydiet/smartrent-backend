package com.smartrent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/**
 * Request DTO for fetching listings within map bounds
 * Used for displaying listings on an interactive map based on visible area
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request để lấy danh sách bài đăng trong vùng hiển thị trên bản đồ")
public class MapBoundsRequest {

    @NotNull(message = "North-East latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
    @Schema(
        description = "Vĩ độ góc Đông Bắc (North-East corner - Trên cùng bên phải)",
        example = "10.823",
        required = true
    )
    BigDecimal neLat;

    @NotNull(message = "North-East longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
    @Schema(
        description = "Kinh độ góc Đông Bắc (North-East corner - Trên cùng bên phải)",
        example = "106.701",
        required = true
    )
    BigDecimal neLng;

    @NotNull(message = "South-West latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
    @Schema(
        description = "Vĩ độ góc Tây Nam (South-West corner - Dưới cùng bên trái)",
        example = "10.705",
        required = true
    )
    BigDecimal swLat;

    @NotNull(message = "South-West longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
    @Schema(
        description = "Kinh độ góc Tây Nam (South-West corner - Dưới cùng bên trái)",
        example = "106.590",
        required = true
    )
    BigDecimal swLng;

    @NotNull(message = "Zoom level is required")
    @Min(value = 1, message = "Zoom must be >= 1")
    @Max(value = 22, message = "Zoom must be <= 22")
    @Schema(
        description = "Mức zoom hiện tại của bản đồ (1-22). Được sử dụng để tối ưu số lượng kết quả trả về.",
        example = "14",
        required = true,
        minimum = "1",
        maximum = "22"
    )
    Integer zoom;

    @Schema(
        description = "Giới hạn số lượng kết quả trả về (mặc định: 100, tối đa: 500)",
        example = "100",
        defaultValue = "100"
    )
    @Builder.Default
    Integer limit = 100;

    @Schema(
        description = "Chỉ lấy bài đăng đã verified (mặc định: false - lấy tất cả)",
        example = "false",
        defaultValue = "false"
    )
    @Builder.Default
    Boolean verifiedOnly = false;

    @Schema(
        description = "Loại danh mục (categoryId) để lọc thêm",
        example = "1",
        nullable = true
    )
    Long categoryId;

    @Schema(
        description = "Loại VIP để lọc thêm (NORMAL, SILVER, GOLD, DIAMOND)",
        example = "GOLD",
        nullable = true
    )
    String vipType;
}