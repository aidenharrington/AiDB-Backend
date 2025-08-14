package com.aidb.aidb_backend.orchestrator;

import com.aidb.aidb_backend.model.dto.ExcelDataDTO;
import com.aidb.aidb_backend.model.dto.ProjectDTO;
import com.aidb.aidb_backend.model.postgres.Project;
import com.aidb.aidb_backend.service.database.postgres.ExcelUploadService;
import com.aidb.aidb_backend.service.database.postgres.ProjectService;
import com.aidb.aidb_backend.service.util.excel.ExcelDataValidatorService;
import com.aidb.aidb_backend.service.util.excel.ExcelParserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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

    public ProjectDTO uploadExcel(String userId, Long projectId, MultipartFile file) throws IOException {
        ExcelDataDTO excelData = parserService.parseExcelFile(file.getInputStream());
        dataValidatorService.validateData(excelData);
        Project project = projectService.getProjectById(userId, projectId);
        excelUploadService.upload(project, excelData);

        return projectService.convertToDto(project);
    }
}
