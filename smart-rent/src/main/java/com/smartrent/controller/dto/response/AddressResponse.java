package com.smartrent.controller.dto.response;

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
public class AddressResponse {
    Long addressId;
    String streetNumber;
    Long streetId;
    String streetName;
    Long wardId;
    String wardName;
    Long districtId;
    String districtName;
    Long provinceId;
    String provinceName;
    String fullAddress;
    BigDecimal latitude;
    BigDecimal longitude;
    boolean isVerified;
}
