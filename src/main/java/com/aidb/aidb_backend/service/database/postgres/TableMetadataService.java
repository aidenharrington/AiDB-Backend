package com.aidb.aidb_backend.service.database.postgres;

import com.aidb.aidb_backend.model.postgres.TableMetadata;
import com.aidb.aidb_backend.repository.TableMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TableMetadataService {

    @Autowired
    private TableMetadataRepository tableMetadataRepository;

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
}
