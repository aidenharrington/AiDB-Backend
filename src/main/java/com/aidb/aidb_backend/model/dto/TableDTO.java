package com.aidb.aidb_backend.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class TableDTO {
    private String id;
    private String fileName;
    private String displayName;
    private String tableName;
    private List<ColumnDTO> columns;
    private List<List<Object>> rows;

    @Data
    public static class ColumnDTO {
        private String name;
        private ColumnTypeDTO type;
    }

    public enum ColumnTypeDTO {
        TEXT,
        NUMBER,
        // Date is stored in user's timezone
        DATE
    }
} 