package com.smartrent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Chat request with conversation history")
public class ChatRequest {

  @Valid
  @NotNull(message = "Messages cannot be null")
  @NotEmpty(message = "Messages cannot be empty")
  @Schema(description = "List of conversation messages", requiredMode = Schema.RequiredMode.REQUIRED)
  List<ChatMessageDTO> messages;

  @Schema(description = "Authenticated user ID (injected by server)", hidden = true)
  String user_id;

  @Schema(description = "JWT auth token for AI service to call authenticated APIs", hidden = true)
  String auth_token;

  @Schema(description = "Listings from previous response for context reference")
  List<LastListingRef> last_listings;

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PRIVATE)
  @Schema(description = "Reference to a listing shown in previous response")
  public static class LastListingRef {
    int position;
    String listingId;
    @Builder.Default
    String title = "";
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PRIVATE)
  @Schema(description = "Single message in a conversation")
  public static class ChatMessageDTO {

    @NotNull(message = "Role cannot be null")
    @Schema(description = "Message role (user or assistant)", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"user", "assistant"})
    String role;

    @NotNull(message = "Content cannot be null")
    @Schema(description = "Message content", requiredMode = Schema.RequiredMode.REQUIRED)
    String content;
  }
}
