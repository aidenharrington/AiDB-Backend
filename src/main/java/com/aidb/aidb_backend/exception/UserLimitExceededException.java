package com.aidb.aidb_backend.exception;

import com.aidb.aidb_backend.exception.http.ForbiddenException;
import com.aidb.aidb_backend.exception.http.HttpException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class UserLimitExceededException extends ForbiddenException {

    public UserLimitExceededException(String message) {
        super(message);
    }


}
