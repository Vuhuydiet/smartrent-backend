package com.smartrent.infra.exception;

import com.smartrent.infra.exception.model.DomainCode;

public class RoleNotFoundException extends DomainException {

  public RoleNotFoundException() {
    super(DomainCode.ROLE_NOT_FOUND);
  }
}

