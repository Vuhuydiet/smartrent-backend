package com.smartrent.infra.exception;

import com.smartrent.infra.exception.model.DomainCode;

public class VerifyCodeExpiredException extends DomainException {

  public VerifyCodeExpiredException() {
    super(DomainCode.VERIFY_CODE_EXPIRED);
  }
}
