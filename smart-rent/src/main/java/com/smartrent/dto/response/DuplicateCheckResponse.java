package com.smartrent.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DuplicateCheckResponse {
    boolean isDuplicate;
    double highestScore;
    String decision;  // PASS, SUSPICIOUS, DUPLICATE
    List<SuspiciousMatch> suspiciousMatches;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SuspiciousMatch {
        Object listingId;
        String title;
        double score;
        double titleSimilarity;
        double descriptionSimilarity;
        double addressSimilarity;
        double priceSimilarity;
        Double llmScore;
        String llmReason;
    }
}
