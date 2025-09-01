package com.aidb.aidb_backend.repository;

import com.aidb.aidb_backend.model.postgres.TableMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    List<TableMetadata> findByProject_IdAndUserId(Long projectId, String userId);


    @Query("SELECT tm.id FROM TableMetadata tm WHERE tm.project.id = :projectId AND tm.userId = :userId")
    List<Long> findIdsByProjectIdAndUserId(Long projectId, String userId);

    @Query("SELECT tm.tableName FROM TableMetadata tm WHERE tm.id = :tableId")
    String findTableNameById(Long tableId);

    boolean existsByIdAndProject_IdAndUserId(Long id, Long projectId, String userId);
}
