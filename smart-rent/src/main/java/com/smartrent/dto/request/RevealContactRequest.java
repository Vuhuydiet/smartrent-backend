package com.smartrent.dto.request;

import com.smartrent.enums.ContactRevealChannel;
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
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RevealContactRequest {

    @NotNull(message = "Channel is required")
    @Schema(description = "Which contact channel is being revealed", example = "PHONE")
    ContactRevealChannel channel;

    @Schema(description = "Optional listing the reveal happened from", example = "123")
    Long listingId;
}
