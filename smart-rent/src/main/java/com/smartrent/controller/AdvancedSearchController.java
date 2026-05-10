package com.smartrent.controller;

import com.smartrent.dto.request.SearchRequest;
import com.smartrent.dto.response.ListingResponse;
import com.smartrent.dto.response.SuggestionResponse;
import com.smartrent.service.ai.AiSearchService;
import com.smartrent.service.discovery.TypoToleranceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/advanced-search")
@RequiredArgsConstructor
public class AdvancedSearchController {

    private final AiSearchService aiSearchService;
    private final TypoToleranceService typoToleranceService;

    /**
     * Phase 3: AI-Powered Free Text Search
     * Handles complex natural language queries.
     */
    @PostMapping
    public ResponseEntity<Page<ListingResponse>> searchNaturalLanguage(
            @RequestBody SearchRequest request,
            Pageable pageable) {
        log.info("Received advanced search request: {}", request.getQuery());
        Page<ListingResponse> results = aiSearchService.searchByNaturalLanguage(request, pageable);
        return ResponseEntity.ok(results);
    }

    /**
     * Phase 4: Typo Tolerance & Autocomplete Suggestions
     * Provides fast suggestions and spell checking for user input.
     */
    @GetMapping("/suggestions")
    public ResponseEntity<List<SuggestionResponse>> getSuggestions(@RequestParam String query) {
        log.info("Received suggestion request for: {}", query);
        List<SuggestionResponse> corrections = typoToleranceService.getTypoCorrections(query);
        return ResponseEntity.ok(corrections);
    }
}
