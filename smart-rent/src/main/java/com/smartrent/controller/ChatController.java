package com.smartrent.controller;

import com.smartrent.dto.request.ChatRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.ChatResponse;
import com.smartrent.service.ai.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/chat")
@Tag(name = "Chat", description = "AI-powered chat for property listing search")
@RequiredArgsConstructor
public class ChatController {

  private final ChatService chatService;

  @PostMapping
  @Operation(
      summary = "Chat with AI assistant",
      description = "Send conversation messages to AI assistant to search and find property listings. " +
                    "The AI can understand natural language queries and search for properties based on user requirements.",
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true,
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ChatRequest.class),
              examples = {
                  @ExampleObject(
                      name = "Simple search query",
                      value = "{ \"messages\": [ { \"role\": \"user\", \"content\": \"I'm looking for a 2-bedroom apartment in Hanoi with a budget around $1000/month\" } ] }"
                  ),
                  @ExampleObject(
                      name = "Multi-turn conversation",
                      value = "{ \"messages\": [ { \"role\": \"user\", \"content\": \"I need an apartment in Ho Chi Minh City\" }, { \"role\": \"assistant\", \"content\": \"I'd be happy to help you find an apartment in Ho Chi Minh City. Could you tell me more about your preferences?\" }, { \"role\": \"user\", \"content\": \"I need 1 bedroom, budget is $600-800, and I prefer District 1\" } ] }"
                  )
              }
          )
      ),
      responses = {
          @io.swagger.v3.oas.annotations.responses.ApiResponse(
              responseCode = "200",
              description = "Successful response",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ChatResponse.class),
                  examples = @ExampleObject(
                      name = "Success",
                      value = "{ \"code\": \"999999\", \"message\": \"Operation completed successfully\", \"data\": { \"message\": { \"role\": \"assistant\", \"content\": \"I found several 2-bedroom apartments in Hanoi within your budget...\" }, \"metadata\": { \"model\": \"gemini-2.0-flash-exp\" } } }"
                  )
              )
          ),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(
              responseCode = "400",
              description = "Invalid request - messages cannot be empty or last message must be from user"
          ),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(
              responseCode = "500",
              description = "Internal server error or AI service unavailable"
          )
      }
  )
  public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
    ChatResponse response = chatService.processChat(request);
    return ApiResponse.<ChatResponse>builder().data(response).build();
  }
}
