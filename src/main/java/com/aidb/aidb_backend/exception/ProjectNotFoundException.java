package com.aidb.aidb_backend.exception;

import com.aidb.aidb_backend.exception.http.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class ProjectNotFoundException extends ResourceNotFoundException {
    public ProjectNotFoundException(Long projectId) {
        super("Project not found with id: " + projectId);
    }
}
