package com.smartrent.dto.request;

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

    String streetNumber;

    @NotNull(message = "Street ID is required")
    Integer streetId;

    @NotNull(message = "Ward ID is required")
    Integer wardId;


    @NotNull(message = "District ID is required")
    Integer districtId;


    @NotNull(message = "Province ID is required")
    Integer provinceId;

    String fullAddress;

    BigDecimal latitude;

    BigDecimal longitude;

    Boolean isVerified;
}
