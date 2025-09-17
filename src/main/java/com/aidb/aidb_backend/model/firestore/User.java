package com.aidb.aidb_backend.model.firestore;

import lombok.Data;

@Data
public class User {

    private String userId;

    private String name;

    private String email;

    public User(String userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
    }
}
