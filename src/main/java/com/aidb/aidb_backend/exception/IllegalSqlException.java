package com.aidb.aidb_backend.exception;

import org.springframework.http.HttpStatus;

public class IllegalSqlException extends RuntimeException {

    private final HttpStatus httpStatus;

    public IllegalSqlException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public IllegalSqlException(String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
