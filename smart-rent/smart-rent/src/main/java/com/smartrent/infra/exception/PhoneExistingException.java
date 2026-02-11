package com.smartrent.infra.exception;

import com.smartrent.infra.exception.model.DomainCode;

public class PhoneExistingException extends DomainException {
  public PhoneExistingException() {
    super(DomainCode.PHONE_EXISTING);
  }
}
