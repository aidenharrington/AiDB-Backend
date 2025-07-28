package com.aidb.aidb_backend.repository;

import com.aidb.aidb_backend.model.postgres.Project;
import com.aidb.aidb_backend.model.postgres.TableMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TableMetadataRepository extends JpaRepository<TableMetadata, UUID> {

    boolean existsByProjectAndName(Project project, String name);

}
