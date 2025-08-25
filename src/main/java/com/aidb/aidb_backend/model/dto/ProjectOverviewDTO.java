package com.aidb.aidb_backend.model.dto;

import lombok.Data;


@Data
public class ProjectOverviewDTO {

    private String id;

    private String name;

    private String userId;

    public ProjectOverviewDTO(Long id, String name, String userId) {
        this.id = id != null ? id.toString() : null;
        this.name = name;
        this.userId = userId;
    }
}
