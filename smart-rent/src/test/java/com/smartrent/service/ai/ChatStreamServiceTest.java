package com.smartrent.service.ai;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.smartrent.dto.request.ChatRequest;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

class ChatStreamServiceTest {

  private WireMockServer wireMock;
  private ChatStreamService service;

  @BeforeEach
  void setUp() {
    wireMock = new WireMockServer(0);
    wireMock.start();
    WebClient client = WebClient.builder().baseUrl("http://localhost:" + wireMock.port()).build();
    service = new ChatStreamService(client);
  }

  @AfterEach
  void tearDown() {
    wireMock.stop();
  }

  @Test
  void streamChat_relaysSseEventsFromAiService() {
    wireMock.stubFor(post(urlEqualTo("/api/v1/chat/stream"))
        .willReturn(aResponse()
            .withHeader("Content-Type", "text/event-stream")
            .withBody("event: status\ndata: {\"phase\":\"thinking\"}\n\n"
                + "event: text\ndata: {\"delta\":\"hello\"}\n\n"
                + "event: done\ndata: {}\n\n")));

    ChatRequest request = new ChatRequest();
    request.setMessages(List.of());

    StepVerifier.create(service.streamChat(request).map(ServerSentEvent::event))
        .expectNext("status")
        .expectNext("text")
        .expectNext("done")
        .verifyComplete();
  }
}
