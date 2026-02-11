package com.smartrent.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Response DTO for address merge history
 * Shows the new address and all legacy addresses that were merged into it
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddressMergeHistoryResponse {

    /**
     * The new address (Province → Ward)
     */
    @JsonProperty("new_address")
    NewFullAddressResponse newAddress;

    /**
     * List of legacy addresses that were merged into this new address
     */
    @JsonProperty("legacy_sources")
    List<LegacyAddressMapping> legacySources;

    /**
     * Total count of legacy addresses merged
     */
    @JsonProperty("total_merged_count")
    Integer totalMergedCount;

    /**
     * Summary note about the merge
     */
    @JsonProperty("merge_note")
    String mergeNote;

    /**
     * Individual legacy address mapping information
     */
    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class LegacyAddressMapping {

        /**
         * Legacy address (Province → District → Ward)
         */
        @JsonProperty("legacy_address")
        FullAddressResponse legacyAddress;

        /**
         * Whether this was a merged province
         */
        @JsonProperty("is_merged_province")
        Boolean isMergedProvince;

        /**
         * Whether this was a merged ward
         */
        @JsonProperty("is_merged_ward")
        Boolean isMergedWard;

        /**
         * Whether this was a divided ward
         */
        @JsonProperty("is_divided_ward")
        Boolean isDividedWard;

        /**
         * Whether this is the default/primary mapping
         */
        @JsonProperty("is_default")
        Boolean isDefault;

        /**
         * Description of the merge type
         */
        @JsonProperty("merge_description")
        String mergeDescription;
    }
}
