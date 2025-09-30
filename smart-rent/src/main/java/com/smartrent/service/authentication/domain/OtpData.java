package com.smartrent.service.authentication.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OtpData {
    
    String otpCode;
    String userId;
    String userEmail;
    LocalDateTime expirationTime;
    LocalDateTime createdTime;
    
    @JsonIgnore
    public boolean isExpired() {
        return expirationTime.isBefore(LocalDateTime.now());
    }
}
