package com.aidb.aidb_backend.repository;

import com.aidb.aidb_backend.model.postgres.Project;
import com.aidb.aidb_backend.model.postgres.TableMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface TableMetadataRepository extends JpaRepository<TableMetadata, Long> {
    // TODO - t&e-refactor - remove
    boolean existsByProjectAndDisplayName(Project project, String displayName);

    @Query("""
    SELECT t.tableName
    FROM TableMetadata t
    JOIN Project p ON t.projectId = p.id
    WHERE t.projectId = :projectId AND p.userId = :userId
""")
    Set<String> findTableNamesByProjectIdAndUserId(@Param("userId") String userId,
                                                   @Param("projectId") Long projectId);
}
