package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Listing report response")
public class ListingReportResponse {

    @Schema(description = "Report ID", example = "1")
    Long reportId;

    @Schema(description = "Listing ID", example = "123")
    Long listingId;

    @Schema(description = "Reporter's name", example = "Nguyễn Văn A")
    String reporterName;

    @Schema(description = "Reporter's phone number", example = "0912345678")
    String reporterPhone;

    @Schema(description = "Reporter's email", example = "reporter@example.com")
    String reporterEmail;

    @Schema(description = "List of report reasons")
    List<ReportReasonResponse> reportReasons;

    @Schema(description = "Additional feedback", example = "Giá không đúng với thực tế")
    String otherFeedback;

    @Schema(description = "Report category", example = "LISTING", allowableValues = {"LISTING", "MAP"})
    String category;

    @Schema(description = "Report creation timestamp")
    LocalDateTime createdAt;
}

