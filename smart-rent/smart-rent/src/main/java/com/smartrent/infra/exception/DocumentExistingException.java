package com.smartrent.infra.exception;

import com.smartrent.infra.exception.model.DomainCode;

public class DocumentExistingException extends DomainException {

  public DocumentExistingException() {
    super(DomainCode.DOCUMENT_EXISTING);
  }
}
