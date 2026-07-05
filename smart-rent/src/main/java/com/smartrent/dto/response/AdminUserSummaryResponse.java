package com.smartrent.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "User summary for admin user list view")
public class AdminUserSummaryResponse {

    String userId;
    String phoneCode;
    String phoneNumber;
    String email;
    String firstName;
    String lastName;
    LocalDateTime createdAt;
    String taxNumber;
    String contactPhoneNumber;
    String avatarUrl;
    Long avatarMediaId;
    Boolean isBroker;

    @Schema(description = "Broker verification status", example = "NONE", allowableValues = { "NONE", "PENDING", "APPROVED", "REJECTED" })
    String brokerVerificationStatus;
}
