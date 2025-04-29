package com.aidb.aidb_backend.service.database.postgres;

import com.aidb.aidb_backend.model.dto.ExcelDataDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserFileDataService {

    // TODO - Skeleton - use docker database

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void createTablesAndInsertData(ExcelDataDto excelData) {
        for (ExcelDataDto.TableDto table : excelData.getTables()) {
            String createTableSql = generateCreateTableSql(table);
            jdbcTemplate.execute(createTableSql);

            // Insert the rows into the database
            for (List<Object> row : table.getRows()) {
                String insertSql = generateInsertSql(table, row);
                jdbcTemplate.update(insertSql);
            }
        }
    }

    private String generateCreateTableSql(ExcelDataDto.TableDto table) {
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS " + table.getName() + " (");

        for (ExcelDataDto.ColumnDto column : table.getColumns()) {
            sql.append(column.getName())
                    .append(" ")
                    .append(mapColumnTypeToSqlType(column.getType()))
                    .append(", ");
        }

        // Remove last comma and close the parenthesis
        sql.setLength(sql.length() - 2);
        sql.append(");");

        return sql.toString();
    }

    private String generateInsertSql(ExcelDataDto.TableDto table, List<Object> row) {
        StringBuilder sql = new StringBuilder("INSERT INTO " + table.getName() + " (");

        // Add columns to insert statement
        for (ExcelDataDto.ColumnDto column : table.getColumns()) {
            sql.append(column.getName()).append(", ");
        }

        sql.setLength(sql.length() - 2);  // Remove last comma
        sql.append(") VALUES (");

        // Add values to insert statement
        for (Object value : row) {
            if (value instanceof String) {
                sql.append("'").append(value).append("', ");
            } else {
                sql.append(value).append(", ");
            }
        }

        sql.setLength(sql.length() - 2);  // Remove last comma
        sql.append(");");

        return sql.toString();
    }

    private String mapColumnTypeToSqlType(ExcelDataDto.ColumnTypeDto columnType) {
        switch (columnType) {
            case TEXT:
                return "TEXT";
            case NUMBER:
                return "DOUBLE PRECISION";
            case DATE:
                return "DATE";
            case ID:
                return "SERIAL PRIMARY KEY";
            default:
                throw new IllegalArgumentException("Unknown column type: " + columnType);
        }
    }
}
