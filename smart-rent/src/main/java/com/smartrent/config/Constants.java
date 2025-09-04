package com.smartrent.config;

public class Constants {
  private Constants() {
  }

  public static final String ROLE_USER = "ROLE_USER";

  public static final String AUTHENTICATION_SERVICE = "AUTHENTICATION_SERVICE";

  public static final String ADMIN_AUTHENTICATION_SERVICE = "ADMIN_AUTHENTICATION_SERVICE";

  public static final String EMAIL_PATTERN = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$";

  public static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&*(){}\\[\\]!~`|])(?=.*\\d).*$";

  public static final int USER_ID_MASKING_INDEX = 6;

  public static final int PHONE_MASKING_INDEX = 6;

  public static final int ID_DOCUMENT_MASKING_INDEX = 3;

  public static final String USER_ID = "user-id";

  public static final String ADMIN_ID = "admin-id";
}
