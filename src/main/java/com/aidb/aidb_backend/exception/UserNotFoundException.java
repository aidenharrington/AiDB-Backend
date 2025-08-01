package com.aidb.aidb_backend.exception;

import com.aidb.aidb_backend.exception.http.ResourceNotFoundException;
import lombok.Getter;

@Getter
public class UserNotFoundException extends ResourceNotFoundException {


    public UserNotFoundException(String message) {
        super(message);
    }


}
