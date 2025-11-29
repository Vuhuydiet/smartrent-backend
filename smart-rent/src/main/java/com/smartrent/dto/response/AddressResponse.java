package com.smartrent.dto.response;

import com.smartrent.infra.repository.entity.AddressMetadata;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/**
 * Address response DTO
 * Contains both formatted address strings and structured metadata
 * Updated to match V45 migration - supports nested legacy/new address structure
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Thông tin địa chỉ chi tiết")
public class AddressResponse {

    @Schema(description = "ID địa chỉ", example = "501")
    Long addressId;

    @Schema(description = "Địa chỉ đầy đủ (cấu trúc cũ - 63 tỉnh, 3 cấp)", example = "123 Nguyễn Trãi, Phường 5, Quận 5, Thành Phố Hồ Chí Minh")
    String fullAddress;

    @Schema(description = "Địa chỉ đầy đủ (cấu trúc mới - 34 tỉnh, 2 cấp)", example = "123 Nguyễn Trãi, Phường Bến Nghé, Thành Phố Hồ Chí Minh")
    String fullNewAddress;

    @Schema(description = "Vĩ độ", example = "10.7545")
    BigDecimal latitude;

    @Schema(description = "Kinh độ", example = "106.6679")
    BigDecimal longitude;

    @Schema(description = "Loại địa chỉ (OLD: cấu trúc cũ 63 tỉnh, NEW: cấu trúc mới 34 tỉnh)", example = "OLD", allowableValues = {"OLD", "NEW"})
    AddressMetadata.AddressType addressType;

    // ==================== CẤU TRÚC ĐỊA CHỈ CŨ (63 tỉnh, 3 cấp) ====================
    @Schema(description = "ID tỉnh/thành phố (cấu trúc cũ)", example = "79")
    Integer legacyProvinceId;

    @Schema(description = "Tên tỉnh/thành phố (cấu trúc cũ)", example = "Thành Phố Hồ Chí Minh")
    String legacyProvinceName;

    @Schema(description = "ID quận/huyện (cấu trúc cũ)", example = "760")
    Integer legacyDistrictId;

    @Schema(description = "Tên quận/huyện (cấu trúc cũ)", example = "Quận 5")
    String legacyDistrictName;

    @Schema(description = "ID phường/xã (cấu trúc cũ)", example = "10777")
    Integer legacyWardId;

    @Schema(description = "Tên phường/xã (cấu trúc cũ)", example = "Phường 5")
    String legacyWardName;

    @Schema(description = "Tên đường (cấu trúc cũ)", example = "Nguyễn Trãi")
    String legacyStreet;

    // ==================== CẤU TRÚC ĐỊA CHỈ MỚI (34 tỉnh, 2 cấp) ====================
    @Schema(description = "Mã tỉnh/thành phố (cấu trúc mới)", example = "79")
    String newProvinceCode;

    @Schema(description = "Tên tỉnh/thành phố (cấu trúc mới)", example = "Thành Phố Hồ Chí Minh")
    String newProvinceName;

    @Schema(description = "Mã phường/xã (cấu trúc mới)", example = "26734")
    String newWardCode;

    @Schema(description = "Tên phường/xã (cấu trúc mới)", example = "Phường Bến Nghé")
    String newWardName;

    @Schema(description = "Tên đường (cấu trúc mới)", example = "Nguyễn Trãi")
    String newStreet;

    // ==================== METADATA CHUNG ====================
    @Schema(description = "ID đường phố", example = "1")
    Integer streetId;

    @Schema(description = "Tên đường (Deprecated - sử dụng legacyStreet hoặc newStreet)", deprecated = true)
    String streetName;

    @Schema(description = "ID dự án/tòa nhà/khu phức hợp (tùy chọn)", example = "1")
    Integer projectId;

    @Schema(description = "Tên dự án/tòa nhà", example = "Vinhomes Central Park")
    String projectName;
}

