package com.smartrent.infra.connector;

import com.smartrent.infra.connector.model.GoogleExchangeTokenRequest;
import com.smartrent.infra.connector.model.GoogleExchangeTokenResponse;
import feign.QueryMap;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "google-connect", url = "${feign.client.config.google.auth.url}")
public interface GoogleAuthConnector {
  @PostMapping(
      value = "/token",
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  GoogleExchangeTokenResponse exchangeToken(@QueryMap GoogleExchangeTokenRequest exchangeTokenRequest);
}
