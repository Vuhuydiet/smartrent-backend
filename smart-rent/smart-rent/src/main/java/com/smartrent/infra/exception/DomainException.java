package com.smartrent.infra.exception;

import com.smartrent.infra.exception.model.DomainCode;
import lombok.Getter;

@Getter
public class DomainException extends RuntimeException {

  private final DomainCode domainCode;

  public DomainException(DomainCode domainCode, Object... args) {
    super(String.format(domainCode.getMessage(), args));
    this.domainCode = domainCode;
  }

}
