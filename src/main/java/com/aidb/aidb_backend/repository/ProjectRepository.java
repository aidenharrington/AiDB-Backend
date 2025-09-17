package com.aidb.aidb_backend.repository;

import com.aidb.aidb_backend.model.dto.ProjectOverviewDTO;
import com.aidb.aidb_backend.model.postgres.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("SELECT new com.aidb.aidb_backend.model.dto.ProjectOverviewDTO(p.id, p.name, p.userId) " +
            "FROM Project p " +
            "WHERE p.userId = :userId " +
            "ORDER BY p.createdAt DESC")
    List<ProjectOverviewDTO> findProjectOverviewDTOs(@Param("userId") String userId);

    @Query("""
        SELECT new com.aidb.aidb_backend.model.dto.ProjectOverviewDTO(
            p.id, p.name, p.userId
        )
        FROM Project p
        WHERE p.userId = :userId AND p.id = :projectId
    """)
    ProjectOverviewDTO findProjectOverviewDTO(@Param("userId") String userId, @Param("projectId") Long projectId);


    Optional<Project> findByIdAndUserId(Long id, String userId);

    boolean existsByIdAndUserId(Long id, String userId);
}

