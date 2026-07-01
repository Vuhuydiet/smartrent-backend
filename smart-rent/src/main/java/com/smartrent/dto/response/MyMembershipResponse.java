package com.smartrent.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Response for GET /v1/memberships/my-membership.
 *
 * A user can hold at most two ACTIVE membership records at once:
 *   - current: startDate <= NOW() AND endDate > NOW()  (in use right now)
 *   - queued:  startDate > NOW()                       (waiting to start when current expires)
 *
 * Both may be null (no membership at all, or only one of the two slots filled).
 * The "queued" slot never has benefits — they are granted by the lifecycle job
 * when the slot becomes current.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MyMembershipResponse {
    UserMembershipResponse current;
    UserMembershipResponse queued;
}
