package com.smartrent.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TakeDownResponse {

    Long listingId;
    String userId;
    LocalDateTime takenDownAt;
    String message;
}
