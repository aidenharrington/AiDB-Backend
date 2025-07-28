package com.aidb.aidb_backend.repository;

import com.aidb.aidb_backend.model.postgres.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findByUserId(String userId);
    Optional<Project> findByIdAndUserId(UUID id, String userId);

}

