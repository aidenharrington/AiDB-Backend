package com.aidb.aidb_backend.orchestrator;

import com.aidb.aidb_backend.model.dto.ExcelDataDto;
import com.aidb.aidb_backend.model.dto.ProjectCreateRequest;
import com.aidb.aidb_backend.model.postgres.Project;
import com.aidb.aidb_backend.service.database.postgres.ExcelUploadService;
import com.aidb.aidb_backend.service.database.postgres.ProjectService;
import com.aidb.aidb_backend.service.util.excel.ExcelDataValidatorService;
import com.aidb.aidb_backend.service.util.excel.ExcelParserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

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

    public Project uploadExcel(String userId, UUID projectId, MultipartFile file) throws IOException {
        ExcelDataDto excelData = parserService.parseExcelFile(file.getInputStream());
        dataValidatorService.validateData(excelData);
        Project project = projectService.getProjectById(userId, projectId);
        excelUploadService.upload(project, excelData);

        return project;
    }



}
