package com.smartrent.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
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
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Follow relationship state between the current user and a target user")
public class FollowStatusResponse {

    @Schema(description = "Target user the status applies to", example = "u-1234")
    String userId;

    @Schema(description = "True when the current user is following the target")
    Boolean isFollowing;

    @Schema(description = "Total number of followers of the target user", example = "42")
    Long followerCount;
}
