package com.smartrent.infra.client;

import com.smartrent.dto.request.AiParsedCriteriaDto;
import com.smartrent.dto.request.AiSuggestionRequest;
import com.smartrent.dto.request.SearchRequest;
import com.smartrent.dto.response.AiSuggestionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// Reads the same base URL as the rest of the AI integration
// (feign.client.config.smartrent-ai.url) so the search AI fallback is
// reachable in deployed environments, not just on localhost.
@FeignClient(name = "ai-server", url = "${ai.server.url:${AI_SERVICE_BASE_URL:http://localhost:8000}}")
public interface AiServerClient {

    @PostMapping("/api/v1/search/parse")
    AiParsedCriteriaDto parseNaturalLanguage(@RequestBody SearchRequest request);

    @PostMapping("/api/v1/search/suggestions")
    AiSuggestionResponse suggestSearchQueries(@RequestBody AiSuggestionRequest request);
}
