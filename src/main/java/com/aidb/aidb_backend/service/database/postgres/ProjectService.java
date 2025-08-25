package com.aidb.aidb_backend.service.database.postgres;

import com.aidb.aidb_backend.exception.ProjectNotFoundException;
import com.aidb.aidb_backend.model.api.ProjectCreateRequest;
import com.aidb.aidb_backend.model.dto.ProjectDTO;
import com.aidb.aidb_backend.model.dto.ProjectOverviewDTO;
import com.aidb.aidb_backend.model.postgres.Project;
import com.aidb.aidb_backend.repository.ProjectRepository;
import com.aidb.aidb_backend.repository.TableMetadataRepository;
import com.aidb.aidb_backend.service.util.sql.SnowflakeIdGenerator;

import com.aidb.aidb_backend.service.util.sql.ProjectConversionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TableMetadataRepository tableMetadataRepository;

    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Autowired
    ProjectConversionService projectConversionService;

    
    @Autowired
    private JdbcTemplate jdbcTemplate;


    public List<ProjectOverviewDTO> getProjectOverviewDTOs(String userId) {
        return projectRepository.findProjectOverviewDTOs(userId);
    }

    public ProjectOverviewDTO getProjectOverviewDTO(String userId, Long projectId) {
        return projectRepository.findProjectOverviewDTO(userId, projectId);
    }

    public Project getProject(String userId, Long projectId) {
        return projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    public ProjectDTO getProjectDTO(String userId, Long projectId) {
        Project project = getProject(userId, projectId);
        return projectConversionService.convertProjectToDTO(project);
    }

    public Project createProject(String userId, ProjectCreateRequest projectCreateRequest) {
        Project project = new Project();
        project.setId(snowflakeIdGenerator.nextId());
        project.setName(projectCreateRequest.getName());
        project.setUserId(userId);
        project.setCreatedAt(Instant.now());

        return projectRepository.save(project);
    }

    public Set<String> getTableNames(String userId, Long projectId) {
        return tableMetadataRepository.findTableDisplayNames(userId, projectId);
    }
}

