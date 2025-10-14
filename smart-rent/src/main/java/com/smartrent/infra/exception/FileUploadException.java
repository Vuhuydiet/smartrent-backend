package com.smartrent.infra.exception;

import com.smartrent.infra.exception.model.DomainCode;

public class FileUploadException extends DomainException {

  public FileUploadException(String message) {
    super(DomainCode.FILE_UPLOAD_FAILED, message);
  }

}
