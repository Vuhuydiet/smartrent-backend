package com.smartrent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {
    private Long addressId;
    private String streetNumber;
    private Long streetId;
    private String streetName;
    private Long wardId;
    private String wardName;
    private Long districtId;
    private String districtName;
    private Long provinceId;
    private String provinceName;
    private String fullAddress;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private boolean isVerified;
}
