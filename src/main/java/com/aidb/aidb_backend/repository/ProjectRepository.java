package com.aidb.aidb_backend.repository;

import com.aidb.aidb_backend.model.postgres.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByUserId(String userId);
    Optional<Project> findByIdAndUserId(Long id, String userId);
}

