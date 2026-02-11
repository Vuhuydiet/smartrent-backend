package com.smartrent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request to report a listing")
public class ListingReportRequest {

    @NotEmpty(message = "At least one report reason must be selected")
    @Schema(description = "Set of report reason IDs", example = "[1, 2, 3]", required = true)
    Set<Long> reasonIds;

    @Schema(description = "Additional feedback from the reporter", example = "Giá không đúng với thực tế")
    String otherFeedback;

    @Schema(description = "Reporter's full name", example = "Nguyễn Văn A")
    String reporterName;

    @NotBlank(message = "Phone number is required")
    @Schema(description = "Reporter's phone number", example = "0912345678", required = true)
    String reporterPhone;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "Reporter's email", example = "reporter@example.com", required = true)
    String reporterEmail;

    @NotNull(message = "Report category is required")
    @Schema(description = "Category of the report", example = "LISTING", allowableValues = {"LISTING", "MAP"}, required = true)
    String category;
}

