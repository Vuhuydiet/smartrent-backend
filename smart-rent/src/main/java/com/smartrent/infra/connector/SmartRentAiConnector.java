package com.smartrent.infra.connector;

import com.smartrent.infra.connector.model.ChatRequestModel;
import com.smartrent.infra.connector.model.ChatResponseModel;
import com.smartrent.infra.connector.model.CompletionRequestModel;
import com.smartrent.infra.connector.model.CompletionResponseModel;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "smartrent-ai", url = "${feign.client.config.smartrent-ai.url}")
public interface SmartRentAiConnector {
  @PostMapping(value = "/api/v1/chat/chat", consumes = MediaType.APPLICATION_JSON_VALUE)
  ChatResponseModel generateChat(@RequestBody ChatRequestModel request);

  @PostMapping(value = "/api/v1/completion/", consumes = MediaType.APPLICATION_JSON_VALUE)
  CompletionResponseModel generateCompletion(@RequestBody CompletionRequestModel request);
}
