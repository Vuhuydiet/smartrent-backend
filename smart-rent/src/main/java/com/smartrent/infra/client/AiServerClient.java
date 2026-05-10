package com.smartrent.infra.client;

import com.smartrent.dto.request.AiParsedCriteriaDto;
import com.smartrent.dto.request.SearchRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ai-server", url = "${ai.server.url:http://localhost:8000}")
public interface AiServerClient {

    @PostMapping("/api/v1/search/parse")
    AiParsedCriteriaDto parseNaturalLanguage(@RequestBody SearchRequest request);
}
