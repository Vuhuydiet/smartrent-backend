package com.smartrent.infra.exception;

import com.smartrent.infra.exception.model.DomainCode;

public class UserNotBlockEligibleException extends DomainException {

  public UserNotBlockEligibleException(int threshold, long resolvedReports) {
    super(DomainCode.USER_NOT_BLOCK_ELIGIBLE, threshold, resolvedReports);
  }
}
