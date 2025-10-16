package com.smartrent.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MembershipPackageResponse {

    Long membershipId;
    String packageCode;
    String packageName;
    String packageLevel;
    Integer durationMonths;
    BigDecimal originalPrice;
    BigDecimal salePrice;
    BigDecimal discountPercentage;
    Boolean isActive;
    String description;
    List<MembershipPackageBenefitResponse> benefits;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}

