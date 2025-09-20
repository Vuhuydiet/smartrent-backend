package com.smartrent.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class AddressCreationRequest {
    @NotBlank(message = "Street number is required")
    String streetNumber;

    @NotNull(message = "Street ID is required")
    Long streetId;

    @NotNull(message = "Ward ID is required")
    Long wardId;

    @NotNull(message = "District ID is required")
    Long districtId;

    @NotNull(message = "Province ID is required")
    Long provinceId;

    BigDecimal latitude;
    BigDecimal longitude;
}
