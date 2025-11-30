package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "AI generated listing title and description")
public class ListingDescriptionResponse {
    @Schema(description = "Generated listing title", example = "Căn hộ 2PN view đẹp, đầy đủ nội thất")
    String title;

    @Schema(description = "Generated listing description", example = "Căn hộ hiện đại với 2 phòng ngủ, 1 phòng tắm, diện tích 78.5m2...")
    String description;
}
