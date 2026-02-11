package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Chat response from AI")
public class ChatResponse {

  @Schema(description = "The response message from the AI assistant", requiredMode = Schema.RequiredMode.REQUIRED)
  ChatMessageDTO message;

  @Schema(description = "Additional metadata about the response")
  Map<String, Object> metadata;

  @Schema(description = "Listing search results if the AI performed a property search")
  Map<String, Object> listings;

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PRIVATE)
  @Schema(description = "Single message in a conversation")
  public static class ChatMessageDTO {

    @Schema(description = "Message role (user or assistant)", requiredMode = Schema.RequiredMode.REQUIRED)
    String role;

    @Schema(description = "Message content", requiredMode = Schema.RequiredMode.REQUIRED)
    String content;
  }
}
