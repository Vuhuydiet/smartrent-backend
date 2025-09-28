package com.smartrent.infra.connector;

import com.smartrent.infra.connector.model.GoogleUserDetailResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "outbound-user-client", url = "${feign.client.config.google.client.url}")
public interface GoogleConnector {
  @GetMapping(value = "/oauth2/v1/userinfo")
  GoogleUserDetailResponse getUserDetail(@RequestParam("alt") String alt, @RequestParam("access_token") String accessToken);
}
