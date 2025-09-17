package com.aidb.aidb_backend.exception.http;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class HttpException extends RuntimeException {

    private HttpStatus httpStatus;

    public HttpException(String message) {
        super(message);
    }


}
