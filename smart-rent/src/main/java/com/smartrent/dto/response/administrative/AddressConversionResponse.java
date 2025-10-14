package com.smartrent.dto.response.administrative;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

/**
 * Response DTO for address conversion result
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Result of converting an address from old to new administrative structure")
public class AddressConversionResponse {

    @Schema(description = "Old address information (before July 1, 2025)")
    OldAddressInfo oldAddress;

    @Schema(description = "New address information (after July 1, 2025)")
    NewAddressInfo newAddress;

    @Schema(description = "Whether the conversion was successful", example = "true")
    Boolean conversionSuccessful;

    @Schema(description = "Conversion accuracy percentage (0-100)", example = "100")
    Integer conversionAccuracy;

    @Schema(description = "Notes about the conversion")
    String conversionNote;

    @Schema(description = "Effective date of the new administrative structure", example = "2025-07-01")
    LocalDate conversionDate;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Schema(description = "Old address information")
    public static class OldAddressInfo {

        @Schema(description = "Province ID", example = "1")
        Long provinceId;

        @Schema(description = "Province name", example = "Hà Nội")
        String provinceName;

        @Schema(description = "District ID", example = "5")
        Long districtId;

        @Schema(description = "District name", example = "Quận Ba Đình")
        String districtName;

        @Schema(description = "Ward ID", example = "15")
        Long wardId;

        @Schema(description = "Ward name", example = "Phường Điện Biên")
        String wardName;

        @Schema(description = "Full address in old structure")
        String fullAddress;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Schema(description = "New address information")
    public static class NewAddressInfo {

        @Schema(description = "Province ID", example = "1")
        Long provinceId;

        @Schema(description = "Province name", example = "Hà Nội")
        String provinceName;

        @Schema(description = "Ward ID (directly under province, no district)", example = "201")
        Long wardId;

        @Schema(description = "Ward name", example = "Phường Điện Biên")
        String wardName;

        @Schema(description = "Full address in new structure (no district)")
        String fullAddress;
    }
}
