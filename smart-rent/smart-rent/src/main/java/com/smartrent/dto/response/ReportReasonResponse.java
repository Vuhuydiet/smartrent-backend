package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Report reason response")
public class ReportReasonResponse {

    @Schema(description = "Reason ID", example = "1")
    Long reasonId;

    @Schema(description = "Reason text", example = "Các thông tin về: giá, diện tích, mô tả")
    String reasonText;

    @Schema(description = "Category of the report reason", example = "LISTING", allowableValues = {"LISTING", "MAP"})
    String category;

    @Schema(description = "Display order", example = "1")
    Integer displayOrder;
}

