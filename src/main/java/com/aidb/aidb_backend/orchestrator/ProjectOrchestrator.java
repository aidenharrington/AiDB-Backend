package com.aidb.aidb_backend.orchestrator;

import com.aidb.aidb_backend.exception.ProjectNotFoundException;
import com.aidb.aidb_backend.model.api.ProjectCreateRequest;
import com.aidb.aidb_backend.model.dto.ProjectDTO;
import com.aidb.aidb_backend.model.dto.ProjectOverviewDTO;
import com.aidb.aidb_backend.model.postgres.Project;
import com.aidb.aidb_backend.service.database.postgres.ProjectService;
import com.aidb.aidb_backend.service.database.postgres.TableMetadataService;
import com.aidb.aidb_backend.service.database.postgres.user_created_tables.ExcelUploadService;
import com.aidb.aidb_backend.service.util.excel.ExcelDataValidatorService;
import com.aidb.aidb_backend.service.util.excel.ExcelParserService;
import com.aidb.aidb_backend.service.util.sql.ProjectConversionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Component
public class ProjectOrchestrator {

    @Autowired
    ProjectService projectService;

    @Autowired
    TableMetadataService tableMetadataService;

    @Autowired
    ExcelDataValidatorService dataValidatorService;

    @Autowired
    ExcelParserService parserService;

    @Autowired
    ExcelUploadService excelUploadService;

    @Autowired
    ProjectConversionService projectConversionService;

    private final String SUCCESS_RESPONSE = "Success";


    public ProjectDTO uploadExcel(String userId, String projectIdString, MultipartFile file) throws Exception {
        Long projectId = Long.valueOf(projectIdString);
        ProjectOverviewDTO projectOverview = projectService.getProjectOverviewDTO(userId, projectId);

        if (projectOverview == null) {
            throw new ProjectNotFoundException(projectId);
        }

        Set<String> tableNames = tableMetadataService.getTableNames(userId, projectId);

        ProjectDTO project = parserService.parseExcelFile(projectOverview, tableNames, file.getInputStream());
        dataValidatorService.validateData(project);

        excelUploadService.upload(projectId, project);

        return project;
    }

    public ProjectDTO createProject(String userId, ProjectCreateRequest projectCreateRequest) {
        Project project = projectService.createProject(userId, projectCreateRequest);
        return projectConversionService.convertProjectToDTO(project);
    }

    public ProjectDTO getProjectDTO(String userId, String projectId) {
        return projectService.getProjectDTO(userId, Long.valueOf(projectId));
    }

    public List<ProjectOverviewDTO> getProjectOverviewDTOs(String userId) {
        return projectService.getProjectOverviewDTOs(userId);
    }

    public String deleteProject(String userId, String projectId) {
        projectService.deleteProject(userId, Long.valueOf(projectId));
        return SUCCESS_RESPONSE;
    }

    public String deleteTable(String userId, String projectId, String tableId) {
        tableMetadataService.deleteTable(userId, Long.valueOf(projectId), Long.valueOf(tableId));
        return SUCCESS_RESPONSE;
    }
}
