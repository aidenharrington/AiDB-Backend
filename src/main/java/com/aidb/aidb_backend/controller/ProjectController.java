package com.aidb.aidb_backend.controller;

import com.aidb.aidb_backend.model.api.APIResponse;
import com.aidb.aidb_backend.model.api.ProjectCreateRequest;
import com.aidb.aidb_backend.model.dto.ProjectDTO;
import com.aidb.aidb_backend.model.dto.ProjectOverviewDTO;
import com.aidb.aidb_backend.model.firestore.util.LimitedOperation;
import com.aidb.aidb_backend.model.postgres.Project;
import com.aidb.aidb_backend.orchestrator.ProjectOrchestrator;
import com.aidb.aidb_backend.service.database.postgres.ProjectService;
import com.aidb.aidb_backend.service.util.excel.ExcelRowCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/projects")
public class ProjectController extends BaseController {

    @Autowired
    ProjectOrchestrator projectOrchestrator;

    @Autowired
    ProjectService projectService;

    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);

    @GetMapping
    public ResponseEntity<APIResponse<List<ProjectOverviewDTO>>> getProjects(@RequestHeader("Authorization") String authToken) throws Exception {
        return handleRequest(authToken,
                null,
                -1,
                (userId, args) -> {
                    return projectService.getProjectOverviewDTOsByUserId(userId);
                }
        );
    }

    @PostMapping
    ResponseEntity<APIResponse<Project>> createProject(@RequestHeader("Authorization") String authToken, @RequestBody ProjectCreateRequest projectCreateRequest) throws Exception {
        return handleRequest(authToken,
                LimitedOperation.PROJECT,
                1,
                (userId, args) -> {
                    ProjectCreateRequest createRequest = (ProjectCreateRequest) args[0];

                    return projectService.createProject(userId, projectCreateRequest);
                }, projectCreateRequest
        );
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<APIResponse<ProjectDTO>> getProject(@RequestHeader("Authorization") String authToken, @PathVariable String projectId) throws Exception {
       return handleRequest(authToken,
            null,
               -1,
                (userId, args) -> {
                    Long projectIdLong = (Long) args[0];

                    return projectService.getProjectDTO(userId, projectIdLong);
               }, Long.valueOf(projectId)
               );

    }

    @PostMapping("/{projectId}/upload")
    public ResponseEntity<APIResponse<ProjectDTO>> uploadExcel(@RequestHeader("Authorization") String authToken,
                                                               @PathVariable String projectId,
                                                               @RequestParam("file")MultipartFile file) throws Exception {
        int excelDataRows = ExcelRowCounter.countRows(file);

        return handleRequest(authToken,
                LimitedOperation.PROJECT,
                excelDataRows,
                (userId, args) -> {
                   Long projectIdLong = (Long) args[0];
                   MultipartFile multipartFile = (MultipartFile) args[1];

                   return projectOrchestrator.uploadExcel(userId, projectIdLong, multipartFile);
                }, Long.valueOf(projectId), file
            );
    }
}
