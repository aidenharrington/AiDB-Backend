package com.aidb.aidb_backend.repository;

import com.aidb.aidb_backend.model.postgres.ColumnMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ColumnMetadataRepository extends JpaRepository<ColumnMetadata, Long> {
}

