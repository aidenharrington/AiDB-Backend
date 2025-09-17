package com.aidb.aidb_backend.model.api;

import com.aidb.aidb_backend.model.firestore.User;

import java.io.IOException;

@FunctionalInterface
public interface ActionWithUserId<T> {
    T apply(User user, Object... args) throws Exception;
}
