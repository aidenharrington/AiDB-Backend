package com.aidb.aidb_backend.model.dto;

import lombok.Data;

import java.util.List;


@Data
public class ProjectDto {

    private String id;

    private String name;

    private String userId;

    private List<TableDto> tables;
}
