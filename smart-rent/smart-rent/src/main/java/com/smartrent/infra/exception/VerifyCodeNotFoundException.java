package com.smartrent.infra.exception;

import com.smartrent.infra.exception.model.DomainCode;

public class VerifyCodeNotFoundException extends DomainException {

  public VerifyCodeNotFoundException() {
    super(DomainCode.VERIFY_CODE_NOT_FOUND);
  }
}
