package com.aidb.aidb_backend.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class ExcelDataDTO {

    private String projectName;
    private List<TableDTO> tables;
}
