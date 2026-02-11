package com.smartrent.util;

import java.text.Normalizer;
import java.util.Locale;

public final class TextNormalizer {
    private TextNormalizer() {}

    public static String normalize(String input) {
        if (input == null) {
            return null;
        }
        // Handle Vietnamese đ/Đ before NFD (NFD doesn't decompose these characters)
        String processed = input.replace("đ", "d").replace("Đ", "D");
        String normalized = Normalizer.normalize(processed, Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9\\s]", " ")
            .replaceAll("\\s+", " ")
            .trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public static String compact(String input, int maxLen) {
        String normalized = normalize(input);
        if (normalized == null) {
            return null;
        }
        return normalized.length() > maxLen ? normalized.substring(0, maxLen) : normalized;
    }
}
