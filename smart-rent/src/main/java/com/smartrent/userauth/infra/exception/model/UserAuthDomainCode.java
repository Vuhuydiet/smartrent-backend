package com.smartrent.userauth.infra.exception.model;

import com.smartrent.common.exception.DomainCode;

public enum UserAuthDomainCode implements DomainCode {
    ;

    private final String code;

    private final String message;

    UserAuthDomainCode(String code, String message) {
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
