package com.aidb.aidb_backend.orchestrator;

import com.aidb.aidb_backend.exception.ProjectNotFoundException;
import com.aidb.aidb_backend.model.api.ProjectCreateRequest;
import com.aidb.aidb_backend.model.dto.ProjectDTO;
import com.aidb.aidb_backend.model.dto.ProjectOverviewDTO;
import com.aidb.aidb_backend.model.postgres.Project;
import com.aidb.aidb_backend.service.database.postgres.ExcelUploadService;
import com.aidb.aidb_backend.service.database.postgres.ProjectService;
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
    ExcelDataValidatorService dataValidatorService;

    @Autowired
    ExcelParserService parserService;

    @Autowired
    ExcelUploadService excelUploadService;

    @Autowired
    ProjectConversionService projectConversionService;


    public ProjectDTO uploadExcel(String userId, Long projectId, MultipartFile file) throws Exception {
        ProjectOverviewDTO projectOverview = projectService.getProjectOverviewDTO(userId, projectId);

        if (projectOverview == null) {
            throw new ProjectNotFoundException(projectId);
        }

        Set<String> tableNames = projectService.getTableNames(userId, projectId);

        ProjectDTO project = parserService.parseExcelFile(projectOverview, tableNames, file.getInputStream());
        dataValidatorService.validateData(project);

        excelUploadService.upload(projectId, project);

        return project;
    }

    public ProjectDTO createProject(String userId, ProjectCreateRequest projectCreateRequest) {
        Project project = projectService.createProject(userId, projectCreateRequest);
        return projectConversionService.convertProjectToDTO(project);
    }

    public ProjectDTO getProjectDTO(String userId, Long projectId) {
        return projectService.getProjectDTO(userId, projectId);
    }

    public List<ProjectOverviewDTO> getProjectOverviewDTOs(String userId) {
        return projectService.getProjectOverviewDTOs(userId);
    }
}
