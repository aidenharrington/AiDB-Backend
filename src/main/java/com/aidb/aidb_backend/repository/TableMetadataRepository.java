package com.aidb.aidb_backend.repository;

import com.aidb.aidb_backend.model.postgres.Project;
import com.aidb.aidb_backend.model.postgres.TableMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TableMetadataRepository extends JpaRepository<TableMetadata, Long> {
    boolean existsByProjectAndDisplayName(Project project, String displayName);
}
