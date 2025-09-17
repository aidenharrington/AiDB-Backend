package com.aidb.aidb_backend.exception;

import com.aidb.aidb_backend.exception.http.HttpException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class IllegalSqlException extends HttpException {

    private final HttpStatus httpStatus;

    public IllegalSqlException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

}
