package com.aidb.aidb_backend.exception.http;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class InternalServerErrorException extends HttpException {

    private final HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;


    public InternalServerErrorException(String message) {
        super(message);
    }
}
