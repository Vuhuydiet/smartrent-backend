package com.smartrent.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecommendationItemDto {
    @JsonProperty("listing_id")
    Long listingId;

    @JsonProperty("score")
    Double score;

    @JsonProperty("cf_score")
    Double cfScore;

    @JsonProperty("cbf_score")
    Double cbfScore;
}
