package com.smartrent.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

/**
 * Response for administrative merge history
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MergeHistoryResponse {

    String entityCode;
    String entityName;
    String entityType; // PROVINCE, DISTRICT, WARD
    Boolean isMerged;
    LocalDate mergeDate;

    // If merged, show the parent/new entity
    MergedInto mergedInto;

    // If this is a parent, show what was merged into it
    List<MergedFrom> mergedFrom;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class MergedInto {
        String code;
        String name;
        LocalDate effectiveDate;
        String reason;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class MergedFrom {
        String code;
        String originalName;
        LocalDate mergeDate;
        String reason;
    }
}
