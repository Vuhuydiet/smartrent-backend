package com.smartrent.infra.exception;

import com.smartrent.infra.exception.model.DomainCode;

public class TaxNumberExisting extends DomainException {

  public TaxNumberExisting() {
    super(DomainCode.TAX_NUMBER_EXISTING);
  }
}
