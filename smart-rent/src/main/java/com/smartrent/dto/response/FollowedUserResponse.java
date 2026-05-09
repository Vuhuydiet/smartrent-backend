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

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "A user appearing in a follower or following list")
public class FollowedUserResponse {

    String userId;
    String firstName;
    String lastName;
    String avatarUrl;
    Boolean isBroker;
    String brokerVerificationStatus;

    @Schema(description = "When this follow edge was created")
    LocalDateTime followedAt;
}
