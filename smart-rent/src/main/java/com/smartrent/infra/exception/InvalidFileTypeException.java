package com.smartrent.infra.exception;

import com.smartrent.infra.exception.model.DomainCode;

public class InvalidFileTypeException extends DomainException {

  public InvalidFileTypeException(String allowedTypes) {
    super(DomainCode.INVALID_FILE_TYPE, allowedTypes);
  }

}
