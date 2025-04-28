package com.aidb.aidb_backend.exception;

import org.springframework.http.HttpStatus;

public class OpenAiApiException extends RuntimeException {

    private final HttpStatus httpStatus;

    public OpenAiApiException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public OpenAiApiException(String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
