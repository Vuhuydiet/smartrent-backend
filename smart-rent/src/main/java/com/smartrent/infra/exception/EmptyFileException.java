package com.smartrent.infra.exception;

import com.smartrent.infra.exception.model.DomainCode;

public class EmptyFileException extends DomainException {

  public EmptyFileException() {
    super(DomainCode.EMPTY_FILE);
  }

}
