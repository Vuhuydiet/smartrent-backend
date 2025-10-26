package com.smartrent.dto.response;

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
    Integer addressId;
    String streetNumber;
    Integer streetId;
    String streetName;
    Integer wardId;
    String wardName;
    Integer districtId;
    String districtName;
    Integer provinceId;
    String provinceName;
    String fullAddress;
    BigDecimal latitude;
    BigDecimal longitude;
    boolean isVerified;
}
