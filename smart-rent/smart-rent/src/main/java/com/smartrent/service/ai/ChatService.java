package com.smartrent.service.ai;

import com.smartrent.dto.request.ChatRequest;
import com.smartrent.dto.response.ChatResponse;
import com.smartrent.infra.connector.SmartRentAiConnector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

  private final SmartRentAiConnector aiConnector;

  /**
   * Process chat conversation with AI assistant for listing search.
   *
   * @param request Chat request containing conversation messages
   * @return Chat response from AI assistant
   */
  public ChatResponse processChat(ChatRequest request) {
    log.info("Processing chat request with {} messages", request.getMessages().size());

    try {
      // Forward request to AI service
      ChatResponse response = aiConnector.chat(request);

      log.info("Chat processed successfully");
      return response;

    } catch (Exception e) {
      log.error("Error processing chat request: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to process chat request: " + e.getMessage(), e);
    }
  }
}
