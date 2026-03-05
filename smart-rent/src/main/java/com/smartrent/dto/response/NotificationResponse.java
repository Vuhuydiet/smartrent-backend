package com.smartrent.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResponse {
    Long id;
    String type;
    String title;
    String message;
    Long referenceId;
    String referenceType;
    Boolean isRead;
    LocalDateTime createdAt;
}
