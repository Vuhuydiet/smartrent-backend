package com.smartrent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSuggestionResponse {
    @Builder.Default
    private List<String> suggestions = new ArrayList<>();

    private String normalizedQuery;
}
