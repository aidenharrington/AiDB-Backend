package com.aidb.aidb_backend.model.api;

import java.io.IOException;

@FunctionalInterface
public interface ActionWithUserId<T> {
    T apply(String userId, Object... args) throws Exception;
}
