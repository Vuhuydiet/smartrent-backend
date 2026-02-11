package com.smartrent.exception;

import com.smartrent.infra.exception.DomainException;
import com.smartrent.infra.exception.model.DomainCode;

public class InvalidPageSizeException extends DomainException {

  public InvalidPageSizeException() {
    super(DomainCode.INVALID_PAGE_SIZE);
  }
}
