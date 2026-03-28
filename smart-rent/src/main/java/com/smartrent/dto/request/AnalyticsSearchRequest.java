package com.smartrent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Search/filter criteria for owner listing analytics")
public class AnalyticsSearchRequest {

    @Schema(description = "Search by listing title (case-insensitive contains match)", example = "phòng trọ")
    String keyword;

    @Schema(description = "Page number (0-indexed)", example = "0", defaultValue = "0")
    @Builder.Default
    Integer page = 0;

    @Schema(description = "Page size", example = "10", defaultValue = "10")
    @Builder.Default
    Integer size = 10;
}
