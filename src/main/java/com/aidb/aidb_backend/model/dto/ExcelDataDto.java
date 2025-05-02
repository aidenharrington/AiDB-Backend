package com.aidb.aidb_backend.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class ExcelDataDto {

    private String projectName;
    private List<TableDto> tables;

    @Data
    public static class TableDto {
        private String name;
        private List<ColumnDto> columns;
        private List<List<Object>> rows;
    }

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
