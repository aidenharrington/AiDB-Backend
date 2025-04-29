package com.aidb.aidb_backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ExcelValidationException extends RuntimeException {

    private final HttpStatus httpStatus;

    public ExcelValidationException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public ExcelValidationException(String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

}
