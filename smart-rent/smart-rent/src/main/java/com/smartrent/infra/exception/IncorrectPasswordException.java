package com.smartrent.infra.exception;

import com.smartrent.infra.exception.model.DomainCode;

public class IncorrectPasswordException extends DomainException {

  public IncorrectPasswordException() {
    super(DomainCode.INCORRECT_PASSWORD);
  }
}
