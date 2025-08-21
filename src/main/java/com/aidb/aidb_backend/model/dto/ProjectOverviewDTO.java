package com.aidb.aidb_backend.model.dto;

import lombok.Data;


@Data
public class ProjectOverviewDTO {

    private Long id;

    private String name;

    private String userId;

    public ProjectOverviewDTO(Long id, String name, String userId) {
        this.id = id;
        this.name = name;
        this.userId = userId;
    }
}
