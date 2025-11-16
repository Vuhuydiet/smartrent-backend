package com.smartrent.infra.exception;

import com.smartrent.infra.exception.model.DomainCode;

public class MembershipPackageExistingException extends DomainException {

  public MembershipPackageExistingException() {
    super(DomainCode.MEMBERSHIP_PACKAGE_EXISTING);
  }
}

