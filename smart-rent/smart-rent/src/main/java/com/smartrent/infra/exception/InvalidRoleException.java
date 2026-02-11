package com.smartrent.infra.exception;

import com.smartrent.infra.exception.model.DomainCode;

public class InvalidRoleException extends DomainException {
  public InvalidRoleException() {
    super(DomainCode.INVALID_ROLE);
  }
}
