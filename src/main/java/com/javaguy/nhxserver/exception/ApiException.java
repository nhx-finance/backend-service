package com.javaguy.nhxserver.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public ApiException(HttpStatus httpStatus, String invalidWalletAddress, String s) {
        this.status = httpStatus;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
