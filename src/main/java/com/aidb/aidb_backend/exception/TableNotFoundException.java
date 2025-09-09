package com.aidb.aidb_backend.exception;

import com.aidb.aidb_backend.exception.http.ResourceNotFoundException;

public class TableNotFoundException extends ResourceNotFoundException {
    public TableNotFoundException() {
        super("Table not found.");
    }

    public TableNotFoundException(String tableName) {
        super("Table: " + tableName + " not found.");
    }
}
