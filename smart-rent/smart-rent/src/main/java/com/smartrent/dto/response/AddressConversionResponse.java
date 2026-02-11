package com.smartrent.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Response for address conversion from legacy to new administrative structure
 * Shows both the original legacy address and the converted new address
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddressConversionResponse {

    /**
     * Original legacy address (Province → District → Ward)
     */
    FullAddressResponse legacyAddress;

    /**
     * Converted new address (Province → Ward, no district)
     */
    NewFullAddressResponse newAddress;

    /**
     * Notes about the conversion process and merge type
     */
    String conversionNote;
}
