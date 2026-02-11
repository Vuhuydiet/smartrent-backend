package com.smartrent.infra.exception;

import com.smartrent.infra.exception.model.DomainCode;

public class EmailExistingException extends DomainException {

  public EmailExistingException() {
    super(DomainCode.EMAIL_EXISTING);
  }
}
