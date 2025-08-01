package com.aidb.aidb_backend.service.database.postgres;

import com.aidb.aidb_backend.exception.ProjectNotFoundException;
import com.aidb.aidb_backend.model.dto.ProjectCreateRequest;
import com.aidb.aidb_backend.model.dto.ProjectDto;
import com.aidb.aidb_backend.model.dto.ProjectOverviewDto;
import com.aidb.aidb_backend.model.dto.TableDto;
import com.aidb.aidb_backend.model.postgres.Project;
import com.aidb.aidb_backend.model.postgres.TableMetadata;
import com.aidb.aidb_backend.model.postgres.ColumnMetadata;
import com.aidb.aidb_backend.repository.ProjectRepository;
import com.aidb.aidb_backend.service.util.sql.SnowflakeIdGenerator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Project> getProjectsByUserId(String userId) {
        return projectRepository.findByUserId(userId);
    }

    public List<ProjectOverviewDto> getProjectOverviewDtosByUserId(String userId) {
        List<Project> projects = projectRepository.findByUserId(userId);
        return projects.stream()
                .map(this::convertToOverviewDto)
                .collect(Collectors.toList());
    }

    public List<ProjectDto> getProjectDtosByUserId(String userId) {
        List<Project> projects = projectRepository.findByUserId(userId);
        return projects.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Project getProjectById(String userId, Long projectId) {
        return projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    public ProjectDto getProjectDtoById(String userId, String projectId) {
        Project project = getProjectById(userId, Long.valueOf(projectId));
        return convertToDto(project);
    }

    public ProjectDto getProjectDto(String userId, Long projectId) {
        Project project = getProjectById(userId, projectId);
        return convertToDto(project);
    }

    public Project createProject(String userId, ProjectCreateRequest projectCreateRequest) {
        Project project = new Project();
        project.setId(snowflakeIdGenerator.nextId());
        project.setName(projectCreateRequest.getName());
        project.setUserId(userId);

        System.out.println("Project: " + project);

        return projectRepository.save(project);
    }

    public ProjectDto convertToDto(Project project) {
        ProjectDto dto = new ProjectDto();
        dto.setId(String.valueOf(project.getId()));
        dto.setName(project.getName());
        dto.setUserId(project.getUserId());
        
        if (project.getTables() != null) {
            List<TableDto> tableDtos = project.getTables().stream()
                    .map(this::convertTableToTableDtoWithData)
                    .collect(Collectors.toList());
            dto.setTables(tableDtos);
        }
        
        return dto;
    }

    public ProjectOverviewDto convertToOverviewDto(Project project) {
        ProjectOverviewDto dto = new ProjectOverviewDto();
        dto.setId(String.valueOf(project.getId()));
        dto.setName(project.getName());
        dto.setUserId(project.getUserId());
        return dto;
    }

    private TableDto convertTableToTableDtoWithData(TableMetadata table) {
        TableDto dto = new TableDto();
        
        // Add metadata fields for frontend compatibility
        dto.setId(String.valueOf(table.getId()));
        dto.setFileName(table.getFileName());
        dto.setDisplayName(table.getDisplayName());
        dto.setTableName(table.getTableName());
        
        if (table.getColumns() != null) {
            List<TableDto.ColumnDto> columnDtos = table.getColumns().stream()
                    .map(this::convertColumnToTableColumnDto)
                    .collect(Collectors.toList());
            dto.setColumns(columnDtos);
        }
        
        // Fetch actual table data using the tableName
        try {
            String selectSql = "SELECT * FROM \"" + table.getTableName() + "\"";
            System.out.println("selectSql: " + selectSql);  
            System.out.println("Using default JdbcTemplate (App User connection)");
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(selectSql);
            System.out.println("rows: " + rows);
            
            // Convert Map<String, Object> to List<Object> for each row
            List<List<Object>> tableRows = rows.stream()
                    .map(row -> row.values().stream().collect(Collectors.toList()))
                    .collect(Collectors.toList());
            
            dto.setRows(tableRows);
            System.out.println("tableRows: " + tableRows);
        } catch (Exception e) {
            // If table doesn't exist or other error, set empty rows
            System.out.println("Error: " + e.getMessage());
            dto.setRows(List.of());
        }
        
        return dto;
    }

    private TableDto.ColumnDto convertColumnToTableColumnDto(ColumnMetadata column) {
        TableDto.ColumnDto dto = new TableDto.ColumnDto();
        dto.setName(column.getName());
        dto.setType(TableDto.ColumnTypeDto.TEXT); // Default to TEXT, you may need to map this properly
        return dto;
    }
}

