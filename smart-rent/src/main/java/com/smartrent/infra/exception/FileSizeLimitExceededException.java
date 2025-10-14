package com.smartrent.infra.exception;

import com.smartrent.infra.exception.model.DomainCode;

public class FileSizeLimitExceededException extends DomainException {

  public FileSizeLimitExceededException(long maxSizeMB) {
    super(DomainCode.FILE_SIZE_EXCEEDED, maxSizeMB);
  }

}
