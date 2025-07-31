package com.aidb.aidb_backend.exception;

import com.aidb.aidb_backend.exception.http.ResourceNotFoundException;
import lombok.Getter;

import java.util.UUID;

@Getter
public class ProjectNotFoundException extends ResourceNotFoundException {


    public ProjectNotFoundException(UUID projectId) {
        super("Cannot find project: " + projectId.toString());
    }


}
