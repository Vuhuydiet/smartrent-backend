package com.smartrent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request to resolve or reject a listing report")
public class ResolveReportRequest {

    @NotNull(message = "Status is required")
    @Schema(description = "Resolution status", example = "RESOLVED", allowableValues = {"RESOLVED", "REJECTED"}, required = true)
    String status;

    @Schema(description = "Admin notes/comments on the resolution", example = "Verified the issue and contacted the listing owner")
    String adminNotes;
}

