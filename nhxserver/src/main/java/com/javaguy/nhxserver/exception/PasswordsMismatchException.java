package com.javaguy.nhxserver.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PasswordsMismatchException extends RuntimeException {
    public PasswordsMismatchException(String message) {
        super(message);
    }

    public PasswordsMismatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
