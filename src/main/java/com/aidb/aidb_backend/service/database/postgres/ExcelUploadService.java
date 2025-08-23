package com.aidb.aidb_backend.service.database.postgres;

import com.aidb.aidb_backend.model.dto.ExcelDataDTO;
import com.aidb.aidb_backend.model.dto.TableDTO;
import com.aidb.aidb_backend.model.postgres.Project;
import com.aidb.aidb_backend.model.postgres.TableMetadata;
import com.aidb.aidb_backend.model.postgres.ColumnMetadata;


import com.aidb.aidb_backend.repository.TableMetadataRepository;
import com.aidb.aidb_backend.repository.ColumnMetadataRepository;
import com.aidb.aidb_backend.service.util.sql.SnowflakeIdGenerator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;

@Service
public class ExcelUploadService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TableMetadataRepository tableMetadataRepository;

    @Autowired
    private ColumnMetadataRepository columnMetadataRepository;

    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Transactional
    public void upload(Project project, ExcelDataDTO excelData) {
        for (TableDTO tableDto : excelData.getTables()) {

            // Generate unique display name
            String displayName = generateUniqueDisplayName(project, tableDto.getFileName());

            // Generate physical table name using hash
            String tableName = generateTableName(project.getId(), displayName);

            // Save metadata: TableMetadata
            TableMetadata tableMetadata = new TableMetadata();
            tableMetadata.setId(snowflakeIdGenerator.nextId());
            tableMetadata.setProjectId(project.getId());
            tableMetadata.setFileName(tableDto.getFileName());
            tableMetadata.setDisplayName(displayName);
            tableMetadata.setTableName(tableName);
            tableMetadata.setCreatedAt(Instant.now());
            tableMetadataRepository.save(tableMetadata);

            // Save metadata: Columns
            for (TableDTO.ColumnDTO columnDto : tableDto.getColumns()) {
                ColumnMetadata columnMetadata = new ColumnMetadata();
                columnMetadata.setId(snowflakeIdGenerator.nextId());
                columnMetadata.setTableId(tableMetadata.getId());
                columnMetadata.setName(columnDto.getName());
                columnMetadata.setType(columnDto.getType().name());
                columnMetadataRepository.save(columnMetadata);
            }

            // Create physical table
            String createTableSql = generateCreateTableSql(tableName, tableDto);
            jdbcTemplate.execute(createTableSql);

            // Insert data into physical table
            for (List<Object> row : tableDto.getRows()) {
                String insertSql = generateInsertSql(tableName, tableDto, row);
                jdbcTemplate.update(insertSql);
            }
        }
    }

    private String generateUniqueDisplayName(Project project, String baseName) {
        String displayName = baseName;
        int suffix = 1;

        while (tableMetadataRepository.existsByProjectAndDisplayName(project, displayName)) {
            displayName = baseName + " (" + suffix++ + ")";
        }

        return displayName;
    }

    private String generateTableName(Long projectId, String displayName) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String toHash = projectId.toString() + ":" + displayName;
            byte[] hashBytes = digest.digest(toHash.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 5; i++) { // 5 bytes = 10 hex chars
                sb.append(String.format("%02x", hashBytes[i]));
            }
            return "project_" + projectId.toString() + "_table_" + sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate table name hash", e);
        }
    }

    private String generateCreateTableSql(String tableName, TableDTO table) {
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS \"" + tableName + "\" (id SERIAL PRIMARY KEY, ");

        for (TableDTO.ColumnDTO column : table.getColumns()) {
            sql.append("\"").append(column.getName()).append("\" ")
                    .append(mapColumnTypeToSqlType(column.getType()))
                    .append(", ");
        }

        sql.setLength(sql.length() - 2); // remove last comma and space
        sql.append(");");
        return sql.toString();
    }

    private String generateInsertSql(String tableName, TableDTO table, List<Object> row) {
        StringBuilder sql = new StringBuilder("INSERT INTO \"" + tableName + "\" (");

        for (TableDTO.ColumnDTO column : table.getColumns()) {
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

    private String mapColumnTypeToSqlType(TableDTO.ColumnTypeDTO columnType) {
        if (columnType == TableDTO.ColumnTypeDTO.TEXT) {
            return "TEXT";
        } else if (columnType == TableDTO.ColumnTypeDTO.NUMBER) {
            return "DOUBLE PRECISION";
        } else if (columnType == TableDTO.ColumnTypeDTO.DATE) {
            return "DATE";
        } else {
            throw new IllegalArgumentException("Unknown column type: " + columnType);
        }
    }

    // Simple SQL string escape (basic)
    private String escapeSql(String input) {
        return input.replace("'", "''");
    }
}
