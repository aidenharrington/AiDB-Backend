package com.aidb.aidb_backend.controller;

import com.aidb.aidb_backend.model.api.APIResponse;
import com.aidb.aidb_backend.model.api.ProjectCreateRequest;
import com.aidb.aidb_backend.model.dto.ProjectDTO;
import com.aidb.aidb_backend.model.dto.ProjectOverviewDTO;
import com.aidb.aidb_backend.model.firestore.util.LimitedOperation;
import com.aidb.aidb_backend.orchestrator.ProjectOrchestrator;
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

    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);

    @GetMapping
    public ResponseEntity<APIResponse<List<ProjectOverviewDTO>>> getProjects(@RequestHeader("Authorization") String authToken) throws Exception {
        return handleRequest(authToken,
                (user, args) -> projectOrchestrator.getProjectOverviewDTOs(user.getUserId())
        );
    }

    @PostMapping
    ResponseEntity<APIResponse<ProjectDTO>> createProject(@RequestHeader("Authorization") String authToken, @RequestBody ProjectCreateRequest projectCreateRequest) throws Exception {
        return handleRequestWithLimit(authToken,
                LimitedOperation.PROJECT,
                1,
                (user, args) ->
                        projectOrchestrator.createProject(user.getUserId(), projectCreateRequest), projectCreateRequest
        );
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<APIResponse<ProjectDTO>> getProject(@RequestHeader("Authorization") String authToken, @PathVariable String projectId) throws Exception {
        return handleRequest(authToken,
                (user, args) ->
                        projectOrchestrator.getProjectDTO(user.getUserId(), projectId), projectId
               );

    }

    @PostMapping("/{projectId}/upload")
    public ResponseEntity<APIResponse<ProjectDTO>> uploadExcel(@RequestHeader("Authorization") String authToken,
                                                               @PathVariable String projectId,
                                                               @RequestParam("file")MultipartFile file) throws Exception {
        int excelDataRows = ExcelRowCounter.countRows(file);

        return handleRequestWithLimit(authToken,
                LimitedOperation.DATA_ROW,
                excelDataRows,
                (user, args) ->
                        projectOrchestrator.uploadExcel(user.getUserId(), projectId, file), projectId, file
            );
    }

    @DeleteMapping("/{projectId}")
    ResponseEntity<APIResponse<String>> deleteProject(@RequestHeader("Authorization") String authToken,
                                                      @PathVariable String projectId) throws Exception {
        return handleRequestWithLimit(authToken,
                LimitedOperation.PROJECT,
                -1,
                (user, args) ->
                        projectOrchestrator.deleteProject(user.getUserId(), projectId), projectId
        );
    }

    @DeleteMapping("/{projectId}/tables/{tableId}")
    ResponseEntity<APIResponse<String>> deleteTable(@RequestHeader("Authorization") String authToken,
                                                    @PathVariable String projectId,
                                                    @PathVariable String tableId) throws Exception {
        return handleRequestWithLimit(authToken,
                LimitedOperation.PROJECT,
                -1,
                (user, args) ->
                        projectOrchestrator.deleteTable(user.getUserId(), projectId, tableId), projectId, tableId
        );
    }
}
