package com.aidb.aidb_backend.service.database.postgres;

import com.aidb.aidb_backend.exception.TableNotFoundException;
import com.aidb.aidb_backend.model.postgres.TableMetadata;
import com.aidb.aidb_backend.repository.ColumnMetadataRepository;
import com.aidb.aidb_backend.repository.ProjectRepository;
import com.aidb.aidb_backend.repository.TableMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TableMetadataService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TableMetadataRepository tableMetadataRepository;

    @Autowired
    private ColumnMetadataRepository columnMetadataRepository;

    @Autowired
    private ProjectRepository projectRepository;

    public Set<String> getTableNames(String userId, Long projectId) {
        return tableMetadataRepository.findTableDisplayNames(userId, projectId);
    }

    public Map<String, String> getTableNameMapping(String userId, Long projectId) {
        List<TableMetadata> tables = tableMetadataRepository.findByProject_IdAndUserId(projectId, userId);

        return tables.stream()
                .collect(Collectors.toMap(
                        TableMetadata::getDisplayName,
                        TableMetadata::getTableName
                ));
    }

    @Transactional
    public void deleteTable(String userId, Long projectId, Long tableId) {
        // Verify ownership
        if (!tableMetadataRepository.existsByIdAndProject_IdAndUserId(tableId, projectId, userId)) {
            throw new TableNotFoundException();
        }

        deleteTableHelper(tableId);
    }

    /**
     * Internal helper that deletes table data and metadata.
     */
    public void deleteTableHelper(Long tableId) {
        // Get table name before deleting metadata
        String tableName = tableMetadataRepository.findTableNameById(tableId);

        if (tableName == null || tableName.trim().isEmpty() || !tableName.matches("[a-zA-Z0-9_]+")) {
            throw new TableNotFoundException();
        } else {
            // Use quoted identifier to avoid SQL injection / keywords
            String sql = "DROP TABLE IF EXISTS \"" + tableName + "\"";

            jdbcTemplate.execute(sql);
        }

        // Delete column metadata
        columnMetadataRepository.deleteByTableMetadata_Id(tableId);

        // Delete table metadata row
        tableMetadataRepository.deleteById(tableId);
    }
}
