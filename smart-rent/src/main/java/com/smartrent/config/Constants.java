package com.smartrent.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

public class Constants {
  private Constants() {
  }

  @NoArgsConstructor(access = AccessLevel.NONE)
  public static final class CacheNames {
    private static final String USER = "user.";
    private static final String AUTH = "auth.";
    public static final String USER_DETAILS = USER + "details";
    public static final String INVALIDATED_TOKENS = AUTH + "invalidatedTokens";
    public static final String OTP = AUTH + "otp";
  }

  @NoArgsConstructor(access = AccessLevel.NONE)
  public static final class CacheKeys {
    // OTP cache key prefixes
    public static final String OTP_PREFIX = "otp:";
    public static final String USER_PREFIX = "user:";
    public static final String EMAIL_PREFIX = "email:";

    // Key separators
    public static final String KEY_SEPARATOR = ":";

    // Key builders
    public static String buildOtpKey(String userId, String otpCode) {
      return OTP_PREFIX + userId + KEY_SEPARATOR + otpCode;
    }

    public static String buildUserKey(String userId) {
      return USER_PREFIX + userId;
    }

    public static String buildEmailKey(String email) {
      return EMAIL_PREFIX + email;
    }
  }

  public static final String ROLE_USER = "ROLE_USER";

  public static final String AUTHENTICATION_SERVICE = "AUTHENTICATION_SERVICE";

  public static final String ADMIN_AUTHENTICATION_SERVICE = "ADMIN_AUTHENTICATION_SERVICE";

  public static final String EMAIL_PATTERN = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$";

  public static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&*(){}\\[\\]!~`|])(?=.*\\d).*$";

  public static final String VIETNAM_PHONE_PATTERN = "^(09|03|07|08|05)[0-9]{8}$";

  public static final int USER_ID_MASKING_INDEX = 6;

  public static final int PHONE_MASKING_INDEX = 6;

  public static final int ID_DOCUMENT_MASKING_INDEX = 3;

  public static final String USER_ID = "user-id";

  public static final String ADMIN_ID = "admin-id";

  public static final String VERIFICATION_EMAIL_SERVICE = "VERIFICATION_EMAIL_SERVICE";

  public static final String EMAIL_VERIFICATION_HEADER = "Email Verification";
}
