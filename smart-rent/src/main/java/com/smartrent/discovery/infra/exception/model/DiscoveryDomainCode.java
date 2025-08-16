package com.smartrent.discovery.infra.exception.model;

import com.smartrent.common.exception.DomainCode;

public enum DiscoveryDomainCode implements DomainCode {
    ;

    private final String code;

    private final String message;

    DiscoveryDomainCode(String code, String message) {
        this.code = code;
        this.message = message;
    }


    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
