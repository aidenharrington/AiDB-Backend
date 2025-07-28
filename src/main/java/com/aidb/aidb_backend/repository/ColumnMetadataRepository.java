package com.aidb.aidb_backend.repository;

import com.aidb.aidb_backend.model.postgres.ColumnMetadata;
import com.aidb.aidb_backend.model.postgres.TableMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ColumnMetadataRepository extends JpaRepository<ColumnMetadata, UUID> {

    List<ColumnMetadata> findByTable(TableMetadata table);

}

