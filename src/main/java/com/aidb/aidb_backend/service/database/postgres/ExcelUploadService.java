package com.aidb.aidb_backend.service.database.postgres;

import com.aidb.aidb_backend.model.dto.ExcelDataDto;
import com.aidb.aidb_backend.model.postgres.Project;
import com.aidb.aidb_backend.model.postgres.TableMetadata;
import com.aidb.aidb_backend.model.postgres.ColumnMetadata;

import com.aidb.aidb_backend.repository.ProjectRepository;
import com.aidb.aidb_backend.repository.TableMetadataRepository;
import com.aidb.aidb_backend.repository.ColumnMetadataRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;

@Service
public class ExcelUploadService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TableMetadataRepository tableMetadataRepository;

    @Autowired
    private ColumnMetadataRepository columnMetadataRepository;

    @Transactional
    public void upload(Project project, ExcelDataDto excelData) {
        for (ExcelDataDto.TableDto tableDto : excelData.getTables()) {

            // Generate unique logical name
            String logicalName = generateUniqueLogicalName(project, tableDto.getName());

            // Generate physical table name using hash
            String physicalTableName = generatePhysicalTableName(project.getId(), logicalName);

            // Save metadata: TableMetadata
            TableMetadata tableMetadata = new TableMetadata();
            tableMetadata.setId(UUID.randomUUID());
            tableMetadata.setProject(project);
            tableMetadata.setName(logicalName);
            tableMetadataRepository.save(tableMetadata);

            // Save metadata: Columns
            for (ExcelDataDto.ColumnDto columnDto : tableDto.getColumns()) {
                ColumnMetadata columnMetadata = new ColumnMetadata();
                columnMetadata.setId(UUID.randomUUID());
                columnMetadata.setTable(tableMetadata);
                columnMetadata.setName(columnDto.getName());
                columnMetadata.setType(columnDto.getType().name());
                columnMetadataRepository.save(columnMetadata);
            }

            // Create physical table
            String createTableSql = generateCreateTableSql(physicalTableName, tableDto);
            jdbcTemplate.execute(createTableSql);

            // Insert data into physical table
            for (List<Object> row : tableDto.getRows()) {
                String insertSql = generateInsertSql(physicalTableName, tableDto, row);
                jdbcTemplate.update(insertSql);
            }
        }
    }

    private String generateUniqueLogicalName(Project project, String baseName) {
        String logicalName = baseName;
        int suffix = 1;

        while (tableMetadataRepository.existsByProjectAndName(project, logicalName)) {
            logicalName = baseName + " (" + suffix++ + ")";
        }

        return logicalName;
    }

    private String generatePhysicalTableName(UUID projectId, String logicalName) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String toHash = projectId.toString() + ":" + logicalName;
            byte[] hashBytes = digest.digest(toHash.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 5; i++) { // 5 bytes = 10 hex chars
                sb.append(String.format("%02x", hashBytes[i]));
            }
            return "project_" + projectId.toString().replace("-", "") + "_table_" + sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate table name hash", e);
        }
    }

    private String generateCreateTableSql(String physicalTableName, ExcelDataDto.TableDto table) {
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS " + physicalTableName + " (id SERIAL PRIMARY KEY, ");

        for (ExcelDataDto.ColumnDto column : table.getColumns()) {
            sql.append("\"").append(column.getName()).append("\" ")
                    .append(mapColumnTypeToSqlType(column.getType()))
                    .append(", ");
        }

        sql.setLength(sql.length() - 2); // remove last comma and space
        sql.append(");");
        return sql.toString();
    }

    private String generateInsertSql(String physicalTableName, ExcelDataDto.TableDto table, List<Object> row) {
        StringBuilder sql = new StringBuilder("INSERT INTO " + physicalTableName + " (");

        for (ExcelDataDto.ColumnDto column : table.getColumns()) {
            sql.append("\"").append(column.getName()).append("\", ");
        }

        sql.setLength(sql.length() - 2);
        sql.append(") VALUES (");

        for (Object value : row) {
            if (value == null) {
                sql.append("NULL, ");
            } else if (value instanceof String) {
                sql.append("'").append(escapeSql((String) value)).append("', ");
            } else if (value instanceof java.util.Date) {
                // Convert java.util.Date to SQL timestamp string
                java.sql.Timestamp ts = new java.sql.Timestamp(((java.util.Date) value).getTime());
                sql.append("'").append(ts.toString()).append("', ");
            } else {
                sql.append(value).append(", ");
            }
        }

        sql.setLength(sql.length() - 2);
        sql.append(");");

        return sql.toString();
    }

    private String mapColumnTypeToSqlType(ExcelDataDto.ColumnTypeDto columnType) {
        return switch (columnType) {
            case TEXT -> "TEXT";
            case NUMBER -> "DOUBLE PRECISION";
            case DATE -> "DATE";
            default -> throw new IllegalArgumentException("Unknown column type: " + columnType);
        };
    }

    // Simple SQL string escape (basic)
    private String escapeSql(String input) {
        return input.replace("'", "''");
    }
}
