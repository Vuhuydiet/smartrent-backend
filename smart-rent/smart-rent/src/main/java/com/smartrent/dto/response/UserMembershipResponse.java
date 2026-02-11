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
public class UserMembershipResponse {

    Long userMembershipId;
    String userId;
    Long membershipId;
    String packageName;
    String packageLevel;
    LocalDateTime startDate;
    LocalDateTime endDate;
    Integer durationDays;
    Long daysRemaining;
    String status;
    BigDecimal totalPaid;
    List<UserMembershipBenefitResponse> benefits;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}

