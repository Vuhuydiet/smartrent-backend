package com.smartrent.config.ai;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * Feign configuration for the smartrent-ai client, wired via
 * {@code @FeignClient(configuration = ...)} on {@code SmartRentAiConnector}
 * rather than {@code @Configuration}. A {@code @Configuration}-annotated
 * class here would be picked up by Spring's component scan and applied to
 * every Feign client (Google, Brevo, ...), leaking the internal shared
 * secret onto requests that have nothing to do with the AI service.
 *
 * <p>Mirrors {@link AiWebClientConfig}, which sends the same header on the
 * WebClient used for chat streaming.
 */
public class AiFeignClientConfig {

  @Bean
  public RequestInterceptor aiInternalApiKeyInterceptor(
      @Value("${ai.internal-api-key:}") String internalApiKey) {
    return requestTemplate -> {
      if (internalApiKey != null && !internalApiKey.isBlank()) {
        requestTemplate.header("X-Internal-Api-Key", internalApiKey);
      }
    };
  }
}
