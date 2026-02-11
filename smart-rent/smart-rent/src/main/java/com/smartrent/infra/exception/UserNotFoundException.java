package com.smartrent.infra.exception;

import com.smartrent.infra.exception.model.DomainCode;

public class UserNotFoundException extends DomainException {

  public UserNotFoundException() {
    super(DomainCode.USER_NOT_FOUND);
  }
}
