package com.smartrent.service.authentication;

import com.smartrent.service.authentication.domain.OtpData;

import java.util.Optional;

public interface OtpCacheService {

    void storeOtp(OtpData otpData);

    Optional<OtpData> getOtp(String otpCode, String userEmail);

    void removeOtp(String userId, String otpCode);

    void removeUserOtps(String userId);

    boolean isValidOtp(String otpCode, String userEmail);
}
