package com.aidb.aidb_backend.exception.http;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class UnauthorizedException extends HttpException {

    private final HttpStatus httpStatus = HttpStatus.UNAUTHORIZED;


    public UnauthorizedException(String message) {
        super(message);
    }
}
