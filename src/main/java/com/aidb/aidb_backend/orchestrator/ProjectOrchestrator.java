package com.aidb.aidb_backend.orchestrator;

import com.aidb.aidb_backend.exception.ProjectNotFoundException;
import com.aidb.aidb_backend.model.dto.ExcelDataDTO;
import com.aidb.aidb_backend.model.dto.ProjectDTO;
import com.aidb.aidb_backend.model.dto.ProjectOverviewDTO;
import com.aidb.aidb_backend.model.postgres.Project;
import com.aidb.aidb_backend.service.database.postgres.ExcelUploadService;
import com.aidb.aidb_backend.service.database.postgres.ProjectService;
import com.aidb.aidb_backend.service.util.excel.ExcelDataValidatorService;
import com.aidb.aidb_backend.service.util.excel.ExcelParserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

//    public ProjectDTO uploadExcel_Deprecated(String userId, Long projectId, MultipartFile file) throws IOException {
//
//        ExcelDataDTO excelData = parserService.parseExcelFile(file.getInputStream());
//        dataValidatorService.validateData(excelData);
//        Project project = projectService.getProject(userId, projectId);
//        excelUploadService.upload(project, excelData);
//
//        //return projectService.convertProjectToDTO(project);
//         return  new ProjectDTO();
//    }

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
}
