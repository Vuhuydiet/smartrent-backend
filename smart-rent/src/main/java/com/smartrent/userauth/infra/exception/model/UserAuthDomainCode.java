package com.smartrent.userauth.infra.exception.model;

import com.smartrent.common.exception.DomainCode;

public enum UserAuthDomainCode implements DomainCode {
<<<<<<< HEAD
=======
    USER_NOT_FOUND("001", "User not found")
>>>>>>> cbf0b5c8b7e5f97ed992c6bb45574048d15cfa7e
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
