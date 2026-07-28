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
    private static final String LISTING = "listing.";
    public static final String USER_DETAILS = USER + "details";
    public static final String INVALIDATED_TOKENS = AUTH + "invalidatedTokens";
    public static final String OTP = AUTH + "otp";
    public static final String LISTING_SEARCH = LISTING + "search";
    public static final String LISTING_BROWSE = LISTING + "browse";
    public static final String LISTING_DETAIL = LISTING + "detail";
    /** Short-TTL cache for GET /v1/listings/search-suggestions (see application.yml). */
    public static final String LISTING_SUGGESTIONS = LISTING + "suggestions";
    /**
     * Homepage "properties by category" stats (POST /v1/listings/stats/categories).
     * Permanent cache (no TTL) — refreshed once a day at midnight by
     * {@code HomepageStatsCacheScheduler}. Only a handful of keys (categoryIds set × verifiedOnly).
     */
    public static final String LISTING_STATS_CATEGORIES = LISTING + "stats.categories";
    /**
     * Homepage "properties by location" stats (POST /v1/listings/stats/provinces).
     * Permanent cache (no TTL) — refreshed once a day at midnight by
     * {@code HomepageStatsCacheScheduler}.
     */
    public static final String LISTING_STATS_PROVINCES = LISTING + "stats.provinces";
    /**
     * Admin dashboard statistics (POST /v1/listings/admin/list). Aggregates over
     * the entire listings table with no WHERE clause, so it's cached with a short
     * TTL (see application.yml) — every admin list request was paying that full
     * scan on top of the paginated query otherwise, regardless of page/filters.
     */
    public static final String LISTING_STATS_ADMIN = LISTING + "stats.admin";
    /**
     * Short-TTL cache for POST /v1/listings/map-bounds (interactive map pins).
     * Keyed by quantized bounding box + zoom + filters. Map browsing is bursty
     * (pan/zoom around the same city, many concurrent users on the same area),
     * so even a 60s TTL absorbs most repeat hits while keeping pins fresh.
     */
    public static final String LISTING_MAP = LISTING + "map";
    public static final String LISTING_RECOMMENDATION_SIMILAR = LISTING + "recommendation.similar";
    public static final String LISTING_RECOMMENDATION_PERSONALIZED = LISTING + "recommendation.personalized";
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

  public static final String EMAIL_VERIFICATION_HEADER = "Xác minh Email";

  public static final String EMAIL_EXPIRING_LISTING_HEADER = "Tin đăng sắp hết hạn";

  public static final String EMAIL_EXPIRING_MEMBERSHIP_HEADER = "Gói thành viên sắp hết hạn";
}
