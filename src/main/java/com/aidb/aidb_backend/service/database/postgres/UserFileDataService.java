package com.aidb.aidb_backend.service.database.postgres;

import com.aidb.aidb_backend.model.dto.ExcelDataDTO;
import com.aidb.aidb_backend.model.dto.TableDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserFileDataService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void createTablesAndInsertData(ExcelDataDTO excelData) {
        for (TableDTO table : excelData.getTables()) {
            String createTableSql = generateCreateTableSql(table);
            jdbcTemplate.execute(createTableSql);

            // Insert the rows into the database
            for (List<Object> row : table.getRows()) {
                String insertSql = generateInsertSql(table, row);
                jdbcTemplate.update(insertSql);
            }
        }
    }

    private String generateCreateTableSql(TableDTO table) {
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS \"" + table.getTableName() + "\" (");

        for (TableDTO.ColumnDTO column : table.getColumns()) {
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

    private String generateInsertSql(TableDTO table, List<Object> row) {
        StringBuilder sql = new StringBuilder("INSERT INTO \"" + table.getTableName() + "\" (");

        // Add columns to insert statement
        for (TableDTO.ColumnDTO column : table.getColumns()) {
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

    private String mapColumnTypeToSqlType(TableDTO.ColumnTypeDTO columnType) {
        return switch (columnType) {
            case TEXT -> "TEXT";
            case NUMBER -> "DOUBLE PRECISION";
            case DATE -> "DATE";
            default -> throw new IllegalArgumentException("Unknown column type: " + columnType);
        };
    }
}
