package com.devops.paylite.web.rest.errors;

import java.io.Serial;

public class AccountResourceException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public AccountResourceException(String message) {
        super(message);
    }
}
