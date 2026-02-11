package com.smartrent.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Request for batch address conversion
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BatchAddressConversionRequest {

    @NotEmpty(message = "Addresses list cannot be empty")
    @Size(max = 1000, message = "Maximum 1000 addresses per batch")
    List<AddressToConvert> addresses;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AddressToConvert {
        String provinceCode;
        String districtCode;
        String wardCode;
        String referenceId; // Optional client reference
    }
}
