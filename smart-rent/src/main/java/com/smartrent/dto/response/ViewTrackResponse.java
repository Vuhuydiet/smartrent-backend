package com.smartrent.dto.response;

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
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Result of tracking a listing detail page view")
public class ViewTrackResponse {

    @Schema(description = "ID of the listing that was viewed", example = "123")
    Long listingId;

    @Schema(description = "Whether a new view was recorded (false when deduped)", example = "true")
    boolean recorded;

    @Schema(description = "Timestamp of the view", example = "2024-01-15T10:30:00")
    LocalDateTime viewedAt;
}
