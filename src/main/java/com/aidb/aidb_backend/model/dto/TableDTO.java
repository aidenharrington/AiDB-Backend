package com.aidb.aidb_backend.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class TableDTO {
    private String id;
    private String fileName;
    private String displayName;
    private String tableName;
    private List<ColumnDto> columns;
    private List<List<Object>> rows;

    @Data
    public static class ColumnDto {
        private String name;
        private ColumnTypeDto type;
    }

    public enum ColumnTypeDto {
        TEXT,
        NUMBER,
        // Date is stored in user's timezone
        DATE
    }
} 