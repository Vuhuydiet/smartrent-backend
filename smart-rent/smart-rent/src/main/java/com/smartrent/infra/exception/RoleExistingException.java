package com.smartrent.infra.exception;

import com.smartrent.infra.exception.model.DomainCode;

public class RoleExistingException extends DomainException {

  public RoleExistingException() {
    super(DomainCode.ROLE_EXISTING);
  }
}

