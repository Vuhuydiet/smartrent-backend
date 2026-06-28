package com.smartrent.config.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient for the FastAPI AI service. Used to relay the chat SSE stream.
 * Sends the internal shared secret (when configured) so the AI service can
 * reject public callers.
 */
@Configuration
public class AiWebClientConfig {

  @Bean
  public WebClient aiWebClient(
      @Value("${ai.base-url}") String baseUrl,
      @Value("${ai.internal-api-key:}") String internalApiKey) {
    WebClient.Builder builder = WebClient.builder().baseUrl(baseUrl);
    if (internalApiKey != null && !internalApiKey.isBlank()) {
      builder.defaultHeader("X-Internal-Api-Key", internalApiKey);
    }
    return builder.build();
  }
}
