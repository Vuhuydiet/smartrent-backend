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
@Schema(description = "AI generated listing description")
public class ListingDescriptionResponse {
    @Schema(description = "Generated human readable description")
    String generatedDescription;

    @Schema(description = "Conversation id returned by AI service (if any)")
    String conversationId;
}
