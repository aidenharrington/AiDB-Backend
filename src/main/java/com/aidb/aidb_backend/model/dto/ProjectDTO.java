package com.aidb.aidb_backend.model.dto;

import lombok.Data;

import java.util.List;


@Data
public class ProjectDTO {

    private String id;

    private String name;

    private String userId;

    private List<TableDTO> tables;
}
