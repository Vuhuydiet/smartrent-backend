package com.smartrent.dto.request.administrative;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Request DTO for batch address conversion
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request for batch conversion of multiple addresses")
public class BatchConversionRequest {

    @NotNull(message = "Addresses list is required")
    @Size(min = 1, max = 100, message = "Batch size must be between 1 and 100 addresses")
    @Valid
    @Schema(description = "List of addresses to convert (max 100)", required = true)
    List<AddressConversionRequest> addresses;
}
