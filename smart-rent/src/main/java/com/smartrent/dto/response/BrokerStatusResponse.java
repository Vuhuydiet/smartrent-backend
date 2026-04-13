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
@Schema(description = "Current broker registration status for a user")
public class BrokerStatusResponse {

    @Schema(description = "User ID", example = "user-123e4567-e89b-12d3-a456-426614174000")
    String userId;

    @Schema(
            description = "Whether this user is an approved broker",
            example = "false"
    )
    Boolean isBroker;

    @Schema(
            description = "Current broker verification status",
            example = "PENDING",
            allowableValues = {"NONE", "PENDING", "APPROVED", "REJECTED"}
    )
    String brokerVerificationStatus;

    @Schema(
            description = "Timestamp when broker registration was submitted",
            example = "2024-01-15T10:30:00"
    )
    LocalDateTime brokerRegisteredAt;

    @Schema(
            description = "Timestamp when admin approved/rejected the registration",
            example = "2024-01-16T09:00:00"
    )
    LocalDateTime brokerVerifiedAt;

    @Schema(
            description = "Rejection reason provided by admin (only present when status is REJECTED)",
            example = "License could not be verified"
    )
    String brokerRejectionReason;

    @Schema(
            description = "External verification source URL used by admin",
            example = "https://www.nangluchdxd.gov.vn/Canhan?page=2&pagesize=20"
    )
    String brokerVerificationSource;
}
