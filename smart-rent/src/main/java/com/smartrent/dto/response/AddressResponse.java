package com.smartrent.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Thông tin địa chỉ chi tiết (unified format)")
public class AddressResponse {

    @Schema(description = "ID địa chỉ", example = "501")
    Long addressId;

    @Schema(description = "Địa chỉ đầy đủ", example = "123 Nguyễn Trãi, Phường 5, Quận 5, Thành Phố Hồ Chí Minh")
    String fullAddress;

    @Schema(description = "Vĩ độ", example = "10.7545")
    BigDecimal latitude;

    @Schema(description = "Kinh độ", example = "106.6679")
    BigDecimal longitude;

    @Schema(description = "Mã tỉnh/thành phố", example = "79")
    String provinceCode;

    @Schema(description = "Tên tỉnh/thành phố", example = "Thành Phố Hồ Chí Minh")
    String provinceName;

    @Schema(description = "Mã quận/huyện", example = "760")
    String districtCode;

    @Schema(description = "Tên quận/huyện", example = "Quận 5")
    String districtName;

    @Schema(description = "Mã phường/xã", example = "27664")
    String wardCode;

    @Schema(description = "Tên phường/xã", example = "Phường 5")
    String wardName;

    @Schema(description = "Tên đường", example = "Nguyễn Trãi")
    String street;

    @Schema(description = "Địa chỉ đầy đủ theo cấu trúc mới (34 tỉnh)", example = "123 Nguyễn Trãi, Phường 5, Thành Phố Hồ Chí Minh")
    String fullNewAddress;

    @Schema(description = "Loại địa chỉ", example = "NEW", allowableValues = {"OLD", "NEW"})
    String addressType;
}