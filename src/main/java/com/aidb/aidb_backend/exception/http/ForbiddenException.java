package com.aidb.aidb_backend.exception.http;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ForbiddenException extends HttpException {

    private final HttpStatus httpStatus = HttpStatus.FORBIDDEN;


    public ForbiddenException(String message) {
        super(message);
    }
}
