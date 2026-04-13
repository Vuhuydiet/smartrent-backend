package com.smartrent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Admin request to approve or reject a broker verification")
public class BrokerVerificationRequest {

    @NotNull(message = "EMPTY_INPUT")
    @NotBlank(message = "EMPTY_INPUT")
    @Schema(
            description = "Verification action to take",
            example = "APPROVE",
            allowableValues = {"APPROVE", "REJECT"}
    )
    String action;

    @Size(max = 500, message = "Rejection reason must not exceed 500 characters")
    @Schema(
            description = "Reason for rejection (required when action is REJECT)",
            example = "Could not verify broker license on the external registry",
            nullable = true
    )
    String rejectionReason;
}
