package com.aidb.aidb_backend.model.dto;

import lombok.Data;

@Data
public class UserDTO {

    private String userId;

    public UserDTO(String userId) {
        this.userId = userId;
    }
}
