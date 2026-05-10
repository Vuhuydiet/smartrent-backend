package com.smartrent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuggestionResponse {
    private String text;
    private String type; // "KEYWORD", "DISTRICT", "PROVINCE", "TYPO_CORRECTION"
}
