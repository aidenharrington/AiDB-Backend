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
            "FROM Project p WHERE p.userId = :userId")
    List<ProjectOverviewDTO> findProjectOverviewDTOsByUserId(@Param("userId") String userId);



    List<Project> findByUserId(String userId);

    Optional<Project> findByIdAndUserId(Long id, String userId);
}

