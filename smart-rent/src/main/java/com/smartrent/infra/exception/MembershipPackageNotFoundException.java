package com.smartrent.infra.exception;

import com.smartrent.infra.exception.model.DomainCode;

public class MembershipPackageNotFoundException extends DomainException {

  public MembershipPackageNotFoundException() {
    super(DomainCode.MEMBERSHIP_PACKAGE_NOT_FOUND);
  }
}

