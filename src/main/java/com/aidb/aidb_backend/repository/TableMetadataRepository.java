package com.aidb.aidb_backend.repository;

import com.aidb.aidb_backend.model.postgres.Project;
import com.aidb.aidb_backend.model.postgres.TableMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface TableMetadataRepository extends JpaRepository<TableMetadata, Long> {

    @Query("""
    SELECT t.displayName
    FROM TableMetadata t
    JOIN t.project p
    WHERE t.project.id = :projectId AND p.userId = :userId
""")
    Set<String> findTableDisplayNames(@Param("userId") String userId,
                                      @Param("projectId") Long projectId);
}
