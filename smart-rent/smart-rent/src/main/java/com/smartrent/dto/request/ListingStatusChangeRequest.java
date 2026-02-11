package com.smartrent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
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
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request to change listing verification status")
public class ListingStatusChangeRequest {

    @NotNull(message = "Verified status is required")
    @Schema(
        description = "New verification status for the listing",
        example = "true",
        required = true
    )
    Boolean verified;

    @Schema(
        description = "Optional reason for the status change",
        example = "Listing meets all verification requirements"
    )
    String reason;
}
