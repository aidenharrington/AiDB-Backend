package com.aidb.aidb_backend.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class TableMetadataDto {
    private String id;
    private String fileName;
    private String displayName;
    private String tableName;
    private List<ColumnMetadataDTO> columns;
} 