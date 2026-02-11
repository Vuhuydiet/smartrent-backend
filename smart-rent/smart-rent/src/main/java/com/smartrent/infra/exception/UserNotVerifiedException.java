package com.smartrent.infra.exception;

import com.smartrent.infra.exception.model.DomainCode;

public class UserNotVerifiedException extends DomainException {

  public UserNotVerifiedException() {
    super(DomainCode.USER_NOT_VERIFIED);
  }
}
