package com.aidb.aidb_backend.service.database.postgres;

import com.aidb.aidb_backend.exception.ProjectNotFoundException;
import com.aidb.aidb_backend.model.dto.ProjectCreateRequest;
import com.aidb.aidb_backend.model.postgres.Project;
import com.aidb.aidb_backend.repository.ProjectRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Project> getProjectsByUserId(String userId) {
        return projectRepository.findByUserId(userId);
    }

    public Project getProjectById(String userId, UUID projectId) {
        return projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    public Project createProject(String userId, ProjectCreateRequest projectCreateRequest) {
        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setName(projectCreateRequest.getName());
        project.setUserId(userId);

        System.out.println("Project: " + project);


        return projectRepository.save(project);
    }


}

