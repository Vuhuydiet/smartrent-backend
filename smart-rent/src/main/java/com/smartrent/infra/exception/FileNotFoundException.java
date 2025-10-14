package com.smartrent.infra.exception;

import com.smartrent.infra.exception.model.DomainCode;

public class FileNotFoundException extends DomainException {

  public FileNotFoundException(String fileKey) {
    super(DomainCode.FILE_NOT_FOUND, fileKey);
  }

}
