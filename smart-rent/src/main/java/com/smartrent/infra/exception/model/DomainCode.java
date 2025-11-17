package com.smartrent.infra.exception.model;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum DomainCode {
  //    Internal Server Error Undefined 9999
  UNKNOWN_ERROR("9999", HttpStatus.INTERNAL_SERVER_ERROR,"Unknown error"),
  //    Internal Server Error	Developer error 1xxx
  INVALID_KEY("1001", HttpStatus.INTERNAL_SERVER_ERROR, "Invalid key"),
  CANNOT_SEND_EMAIL("1002", HttpStatus.INTERNAL_SERVER_ERROR, "Cannot send email"),
  //    Bad Request Client Input Error 2xxx
  EMPTY_INPUT("2001", HttpStatus.BAD_REQUEST, "This field should not be null or empty"),
  INVALID_PASSWORD("2002", HttpStatus.BAD_REQUEST, "Password should be at least {} characters, " +
      "contain at least one uppercase letter, one lowercase letter, one number, and one special character"),
  INVALID_ROLE("2003", HttpStatus.BAD_REQUEST, "Invalid role"),
  INVALID_EMAIL("2004", HttpStatus.BAD_REQUEST, "Invalid email"),
  INVALID_PHONE("2005", HttpStatus.BAD_REQUEST, "Invalid phone number"),
  VERIFY_CODE_EXPIRED("2006", HttpStatus.BAD_REQUEST, "Verify code expired"),
  INCORRECT_PASSWORD("2007", HttpStatus.BAD_REQUEST, "Incorrect password"),
  PASSWORD_SAME("2008", HttpStatus.BAD_REQUEST, "The same password"),
  INVALID_PAGE("2009", HttpStatus.BAD_REQUEST, "Invalid page"),
  INVALID_PAGE_SIZE("2010", HttpStatus.BAD_REQUEST, "Invalid page size"),
  //    Existing Error 3xxx
  EMAIL_EXISTING("3001", HttpStatus.CONFLICT, "Email already exists"),
  PHONE_EXISTING("3002", HttpStatus.CONFLICT, "Phone already exists"),
  DOCUMENT_EXISTING("3003", HttpStatus.CONFLICT, "Document already exists"),
  TAX_NUMBER_EXISTING("3004", HttpStatus.BAD_REQUEST, "Tax number already exists"),
  //    Not Found Error 4xxx
  USER_NOT_FOUND("4001", HttpStatus.NOT_FOUND, "User not found"),
  VERIFY_CODE_NOT_FOUND("4002", HttpStatus.NOT_FOUND, "Verify code not found"),
  PAYMENT_NOT_FOUND("4003", HttpStatus.NOT_FOUND, "Payment not found: %s"),
  LISTING_NOT_FOUND("4004", HttpStatus.NOT_FOUND, "Listing not found"),
  ADDRESS_NOT_FOUND("4005", HttpStatus.NOT_FOUND, "Address not found"),
  PROVINCE_NOT_FOUND("4006", HttpStatus.NOT_FOUND, "Province not found"),
  DISTRICT_NOT_FOUND("4007", HttpStatus.NOT_FOUND, "District not found"),
  WARD_NOT_FOUND("4008", HttpStatus.NOT_FOUND, "Ward not found"),
  RESOURCE_NOT_FOUND("4009", HttpStatus.NOT_FOUND, "Resource not found: %s"),
  STREET_NOT_FOUND("4010", HttpStatus.NOT_FOUND, "Street not found"),
  PROVINCE_MAPPING_NOT_FOUND("4011", HttpStatus.NOT_FOUND, "Province mapping not found"),
  WARD_MAPPING_NOT_FOUND("4012", HttpStatus.NOT_FOUND, "Ward mapping not found"),
  CATEGORY_NOT_FOUND("4013", HttpStatus.NOT_FOUND, "Category not found"),
  //    Unauthorized	Client	5xxx (Unauthenticated error)
  UNAUTHENTICATED("5001", HttpStatus.UNAUTHORIZED, "Unauthenticated"),
  INVALID_EMAIL_PASSWORD("5002", HttpStatus.UNAUTHORIZED, "Invalid email or password"),
  INVALID_TOKEN("5003", HttpStatus.UNAUTHORIZED, "Invalid token"),
  USER_NOT_VERIFIED("5004", HttpStatus.UNAUTHORIZED, "User not verified"),
  INVALID_OAUTH_CODE("5005", HttpStatus.UNAUTHORIZED, "Invalid or expired OAuth authorization code"),
  OAUTH_AUTHENTICATION_FAILED("5006", HttpStatus.UNAUTHORIZED, "OAuth authentication failed"),
  //    Forbidden	Client	6xxx (Unauthorized error)
  UNAUTHORIZED("6001", HttpStatus.FORBIDDEN, "Don't have permission"),
  //    Payment Error 7xxx
  PAYMENT_VALIDATION_ERROR("7001", HttpStatus.BAD_REQUEST, "Payment validation error: %s"),
  INVALID_PAYMENT_AMOUNT("7002", HttpStatus.BAD_REQUEST, "Invalid payment amount: %s"),
  INVALID_CURRENCY("7003", HttpStatus.BAD_REQUEST, "Invalid currency: %s"),
  INVALID_TRANSACTION_REF("7004", HttpStatus.BAD_REQUEST, "Invalid transaction reference: %s"),
  PAYMENT_PROVIDER_NOT_FOUND("7005", HttpStatus.SERVICE_UNAVAILABLE, "Payment provider not found: %s"),
  PAYMENT_PROVIDER_INVALID_CONFIG("7006", HttpStatus.SERVICE_UNAVAILABLE, "Payment provider configuration is invalid: %s"),
  PAYMENT_OPERATION_NOT_SUPPORTED("7007", HttpStatus.SERVICE_UNAVAILABLE, "%s not supported by %s"),
  PAYMENT_PROVIDER_ERROR("7008", HttpStatus.SERVICE_UNAVAILABLE, "Payment provider error: %s"),
  USER_NOT_AUTHENTICATED("7009", HttpStatus.UNAUTHORIZED, "User not authenticated for payment"),
  //    File Storage Error 8xxx
  INVALID_FILE_TYPE("8001", HttpStatus.BAD_REQUEST, "Invalid file type: %s"),
  FILE_UPLOAD_ERROR("8002", HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file: %s"),
  FILE_READ_ERROR("8003", HttpStatus.BAD_REQUEST, "Failed to read file: %s"),
  //    External Service Error 9xxx
  EXTERNAL_SERVICE_ERROR("9001", HttpStatus.SERVICE_UNAVAILABLE, "External service error"),
  TOO_MANY_REQUESTS("9002", HttpStatus.TOO_MANY_REQUESTS, "Too many requests to external service"),
  BAD_REQUEST_ERROR("9003", HttpStatus.BAD_REQUEST, "Bad request to external service"),
  //    AI Service Error 9100-9199
  AI_SERVICE_ERROR("9101", HttpStatus.INTERNAL_SERVER_ERROR, "AI service internal error"),
  AI_SERVICE_UNAVAILABLE("9102", HttpStatus.SERVICE_UNAVAILABLE, "AI service is unavailable"),
  AI_SERVICE_TIMEOUT("9103", HttpStatus.REQUEST_TIMEOUT, "AI service request timeout"),
  AI_SERVICE_INVALID_RESPONSE("9104", HttpStatus.BAD_GATEWAY, "Invalid response from AI service"),
  //    OTP Error 10xxx
  OTP_INVALID_PHONE("10001", HttpStatus.BAD_REQUEST, "Invalid phone number format"),
  OTP_NON_VIETNAM_PHONE("10002", HttpStatus.BAD_REQUEST, "Only Vietnam phone numbers are supported"),
  OTP_RATE_LIMIT_EXCEEDED("10003", HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded. Please try again later"),
  OTP_GENERATION_FAILED("10004", HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate OTP"),
  OTP_SEND_FAILED("10005", HttpStatus.SERVICE_UNAVAILABLE, "Failed to send OTP via all channels"),
  OTP_NOT_FOUND("10006", HttpStatus.NOT_FOUND, "OTP not found or expired"),
  OTP_INVALID_CODE("10007", HttpStatus.BAD_REQUEST, "Invalid OTP code"),
  OTP_VERIFICATION_ATTEMPTS_EXCEEDED("10008", HttpStatus.TOO_MANY_REQUESTS, "Maximum verification attempts exceeded"),
  OTP_ALREADY_VERIFIED("10009", HttpStatus.BAD_REQUEST, "OTP already verified"),
  OTP_PROVIDER_ERROR("10010", HttpStatus.SERVICE_UNAVAILABLE, "OTP provider error: %s"),
  OTP_INVALID_REQUEST_ID("10011", HttpStatus.BAD_REQUEST, "Invalid request ID"),
  //    Duration Plan Error 11xxx
  DURATION_PLAN_NOT_FOUND("11001", HttpStatus.NOT_FOUND, "Duration plan not found"),
  DURATION_PLAN_DUPLICATE_DURATION("11002", HttpStatus.CONFLICT, "Duration plan with this duration already exists"),
  DURATION_PLAN_LAST_ACTIVE("11003", HttpStatus.BAD_REQUEST, "Cannot deactivate the last active duration plan"),
  //    Listing Creation Error 12xxx
  LISTING_CREATION_CACHE_NOT_FOUND("12001", HttpStatus.NOT_FOUND, "Listing creation request not found in cache"),
  LISTING_CREATION_PAYMENT_FAILED("12002", HttpStatus.PAYMENT_REQUIRED, "Payment failed for listing creation"),
  LISTING_ALREADY_EXISTS_FOR_TRANSACTION("12003", HttpStatus.CONFLICT, "Listing already exists for this transaction")
  ;

  private final String value;

  private final HttpStatus status;

  private final String message;

  DomainCode(String value, HttpStatus status, String message) {
    this.value = value;
    this.status = status;
    this.message = message;
  }

}
