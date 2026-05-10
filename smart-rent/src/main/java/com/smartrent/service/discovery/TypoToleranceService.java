package com.smartrent.service.discovery;

import com.smartrent.dto.response.SuggestionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TypoToleranceService {

    private final StringRedisTemplate redisTemplate;
    private final LevenshteinDistance levenshtein = new LevenshteinDistance();
    
    private final List<String> dictionary = new ArrayList<>();

    @PostConstruct
    public void initDictionary() {
        // In a real scenario, this would be loaded from Redis or Database
        dictionary.addAll(List.of(
                "phòng trọ", "căn hộ", "nhà nguyên căn", "mặt bằng", "văn phòng",
                "quận 1", "quận 2", "quận 3", "quận 4", "quận 5", "quận 6", "quận 7", 
                "quận 8", "quận 9", "quận 10", "quận 11", "quận 12",
                "bình thạnh", "gò vấp", "phú nhuận", "tân bình", "tân phú", "thủ đức",
                "hồ chí minh", "hà nội", "đà nẵng"
        ));
        log.info("Initialized Typo Tolerance dictionary with {} keywords", dictionary.size());
    }

    /**
     * Checks if the query has a typo and suggests corrections using Levenshtein distance.
     * Caches the results in Redis for 24 hours to prevent recalculating distances.
     */
    public List<SuggestionResponse> getTypoCorrections(String input) {
        if (input == null || input.trim().length() < 2) {
            return List.of();
        }

        String normalizedInput = input.trim().toLowerCase();
        String cacheKey = "smartrent:typo_cache:" + normalizedInput;

        // 1. Check Redis Cache
        try {
            Boolean hasKey = redisTemplate.hasKey(cacheKey);
            if (Boolean.TRUE.equals(hasKey)) {
                log.debug("Cache hit for typo correction: {}", normalizedInput);
                List<String> cachedWords = redisTemplate.opsForList().range(cacheKey, 0, -1);
                if (cachedWords != null && !cachedWords.isEmpty()) {
                    return cachedWords.stream()
                        .map(word -> SuggestionResponse.builder()
                                .text(word)
                                .type("TYPO_CORRECTION")
                                .build())
                        .collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            log.warn("Redis cache error: {}", e.getMessage());
        }

        // 2. Calculate using Levenshtein Distance
        List<SuggestionResponse> corrections = dictionary.stream()
                .map(keyword -> {
                    int distance = levenshtein.apply(normalizedInput, keyword);
                    return new Object() {
                        String word = keyword;
                        int dist = distance;
                    };
                })
                .filter(obj -> obj.dist > 0 && obj.dist <= 2)
                .sorted((a, b) -> Integer.compare(a.dist, b.dist))
                .limit(5)
                .map(obj -> SuggestionResponse.builder()
                        .text(obj.word)
                        .type("TYPO_CORRECTION")
                        .build())
                .collect(Collectors.toList());

        // 3. Save to Redis Cache (async fire-and-forget logic simulated here)
        try {
            if (!corrections.isEmpty()) {
                List<String> words = corrections.stream().map(SuggestionResponse::getText).collect(Collectors.toList());
                redisTemplate.opsForList().rightPushAll(cacheKey, words);
                redisTemplate.expire(cacheKey, java.time.Duration.ofHours(24));
            }
        } catch (Exception e) {
            log.warn("Failed to write typo corrections to Redis: {}", e.getMessage());
        }

        return corrections;
    }
}
