package com.smartrent.infra.exception;

import com.smartrent.infra.exception.model.DomainCode;

public class AdminNotFoundException extends DomainException {

  public AdminNotFoundException() {
    super(DomainCode.ADMIN_NOT_FOUND);
  }
}

