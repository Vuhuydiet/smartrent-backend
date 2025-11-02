package com.smartrent.exception;

import com.smartrent.infra.exception.DomainException;
import com.smartrent.infra.exception.model.DomainCode;

public class InvalidPageException extends DomainException {

  public InvalidPageException() {
    super(DomainCode.INVALID_PAGE);
  }
}
