package com.aidb.aidb_backend.exception.http;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ResourceNotFoundException extends HttpException {

    private final HttpStatus httpStatus = HttpStatus.NOT_FOUND;


    public ResourceNotFoundException(String message) {
        super(message);
    }
}
