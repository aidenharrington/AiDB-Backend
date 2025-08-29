package com.aidb.aidb_backend.service.database.postgres.user_created_tables;

import com.aidb.aidb_backend.model.dto.ProjectDTO;
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
import java.util.stream.Collectors;

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
    public void upload(Long projectId, ProjectDTO project) {
        for (TableDTO tableDto : project.getTables()) {

            // Generate unique display name
            String displayName = tableDto.getDisplayName();

            // Generate physical table name using hash
            String tableName = generateTableName(projectId, displayName);

            // Create a stub Project object with only ID so Hibernate doesn't complain
            Project stubProject = new Project();
            stubProject.setId(projectId);

            // Save metadata: TableMetadata
            TableMetadata tableMetadata = new TableMetadata();
            tableMetadata.setId(snowflakeIdGenerator.nextId());
            tableMetadata.setUserId(project.getUserId());
            tableMetadata.setProject(stubProject);
            tableMetadata.setFileName(tableDto.getFileName());
            tableMetadata.setDisplayName(displayName);
            tableMetadata.setTableName(tableName);
            tableMetadata.setCreatedAt(Instant.now());
            tableMetadataRepository.save(tableMetadata);

            // Save metadata: Columns
            for (TableDTO.ColumnDTO columnDto : tableDto.getColumns()) {
                ColumnMetadata columnMetadata = new ColumnMetadata();
                columnMetadata.setId(snowflakeIdGenerator.nextId());
                columnMetadata.setTableMetadata(tableMetadata);
                columnMetadata.setName(columnDto.getName());
                columnMetadata.setType(columnDto.getType().name());
                columnMetadataRepository.save(columnMetadata);
            }

            // Create physical table
            String createTableSql = generateCreateTableSql(tableName, tableDto);
            jdbcTemplate.execute(createTableSql);

            // Insert data into physical table
            for (List<Object> row : tableDto.getRows()) {
                insertTable(tableName, tableDto, row);
            }
        }
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

    private void insertTable(String tableName, TableDTO table, List<Object> row) {
        // Build SQL with placeholders `?` for JdbcTemplate
        String sql = "INSERT INTO \"" + tableName + "\" (" +
                table.getColumns().stream()
                        .map(c -> "\"" + c.getName() + "\"")
                        .collect(Collectors.joining(", ")) +
                ") VALUES (" +
                table.getColumns().stream().map(c -> "?").collect(Collectors.joining(", ")) +
                ")";

        // Execute with JdbcTemplate and pass parameters
        jdbcTemplate.update(sql, row.toArray());
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
