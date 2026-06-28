package com.smartrent.service.ai;

import com.smartrent.dto.request.ChatRequest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

/**
 * Relays the AI service's chat SSE stream so the browser talks only to the
 * backend (authenticated + rate-limited) instead of the AI service directly.
 */
@Service
public class ChatStreamService {

  private static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_TYPE =
      new ParameterizedTypeReference<>() {};

  private final WebClient aiWebClient;

  public ChatStreamService(WebClient aiWebClient) {
    this.aiWebClient = aiWebClient;
  }

  public Flux<ServerSentEvent<String>> streamChat(ChatRequest request) {
    return aiWebClient.post()
        .uri("/api/v1/chat/stream")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.TEXT_EVENT_STREAM)
        .bodyValue(request)
        .retrieve()
        .bodyToFlux(SSE_TYPE);
  }
}
