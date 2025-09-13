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
  //    Existing Error 3xxx
  EMAIL_EXISTING("3001", HttpStatus.CONFLICT, "Email already exists"),
  PHONE_EXISTING("3002", HttpStatus.CONFLICT, "Phone already exists"),
  DOCUMENT_EXISTING("3003", HttpStatus.CONFLICT, "Document already exists"),
  TAX_NUMBER_EXISTING("3004", HttpStatus.BAD_REQUEST, "Tax number already exists"),
  //    Not Found Error 4xxx
  USER_NOT_FOUND("4001", HttpStatus.NOT_FOUND, "User not found"),
  VERIFY_CODE_NOT_FOUND("4002", HttpStatus.NOT_FOUND, "Verify code not found"),
  //    Unauthorized	Client	5xxx (Unauthenticated error)
  UNAUTHENTICATED("5001", HttpStatus.UNAUTHORIZED, "Unauthenticated"),
  INVALID_EMAIL_PASSWORD("5002", HttpStatus.UNAUTHORIZED, "Invalid email or password"),
  INVALID_TOKEN("5003", HttpStatus.UNAUTHORIZED, "Invalid token"),
  USER_NOT_VERIFIED("5004", HttpStatus.UNAUTHORIZED, "User not verified"),
  //    Forbidden	Client	6xxx (Unauthorized error)
  UNAUTHORIZED("6001", HttpStatus.FORBIDDEN, "Don't have permission")
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
