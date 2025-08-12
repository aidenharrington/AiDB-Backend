package com.aidb.aidb_backend.controller;

import com.aidb.aidb_backend.exception.ExcelValidationException;
import com.aidb.aidb_backend.exception.http.HttpException;
import com.aidb.aidb_backend.model.api.ProjectCreateRequest;
import com.aidb.aidb_backend.model.dto.ProjectDTO;
import com.aidb.aidb_backend.model.postgres.Project;
import com.aidb.aidb_backend.orchestrator.ProjectOrchestrator;
import com.aidb.aidb_backend.security.authorization.FirebaseAuthService;
import com.aidb.aidb_backend.service.database.postgres.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    @Autowired
    ProjectOrchestrator projectOrchestrator;

    @Autowired
    private FirebaseAuthService firebaseAuthService;

    @Autowired
    ProjectService projectService;

    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);

    @GetMapping
    public ResponseEntity<Object> getProjects(@RequestHeader("Authorization") String authToken) {
        try {
            String userId = firebaseAuthService.authorizeUser(authToken);

            List<ProjectDTO> projects = projectService.getProjectDtosByUserId(userId);

            return ResponseEntity.ok(projects);
        } catch (HttpException e) {
            logger.error(e.getMessage(), e.getHttpStatus());
            return new ResponseEntity<>(e.getMessage(), e.getHttpStatus());
        } catch (Exception e) {
            logger.error(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            return new ResponseEntity<>("Unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    ResponseEntity<Object> createProject(@RequestHeader("Authorization") String authToken, @RequestBody ProjectCreateRequest projectCreateRequest) {
        try {
            String userId = firebaseAuthService.authorizeUser(authToken);

            Project project = projectService.createProject(userId, projectCreateRequest);

            return ResponseEntity.ok(project);
        } catch (HttpException e) {
            logger.error(e.getMessage(), e.getHttpStatus());
            return new ResponseEntity<>(e.getMessage(), e.getHttpStatus());
        } catch (Exception e) {
            logger.error(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            return new ResponseEntity<>("Unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<Object> getProject(@RequestHeader("Authorization") String authToken, @PathVariable String projectId) {
        try {
            String userId = firebaseAuthService.authorizeUser(authToken);

            ProjectDTO project = projectService.getProjectDtoById(userId, projectId);

            return ResponseEntity.ok(project);
        } catch (HttpException e) {
            logger.error(e.getMessage(), e.getHttpStatus());
            return new ResponseEntity<>(e.getMessage(), e.getHttpStatus());
        } catch (Exception e) {
            logger.error(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            return new ResponseEntity<>("Unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/{projectId}/upload")
    public ResponseEntity<Object> uploadExcel(@RequestHeader("Authorization") String authToken, @PathVariable String projectId, @RequestParam("file")MultipartFile file){
        try {
            String userId = firebaseAuthService.authorizeUser(authToken);

            Project project = projectOrchestrator.uploadExcel(userId, Long.valueOf(projectId), file);
            
            // Convert Project entity to ProjectDTO before returning
            ProjectDTO projectDto = projectService.convertToDto(project);

            return ResponseEntity.ok(projectDto);
        } catch (HttpException e) {
            logger.error(e.getMessage(), e.getHttpStatus());
            return new ResponseEntity<>(e.getMessage(), e.getHttpStatus());
        } catch (ExcelValidationException e) {
            logger.error(e.getMessage(), e.getHttpStatus());
            return new ResponseEntity<>(e.getMessage(), e.getHttpStatus());
        } catch (Exception e) {
            logger.error(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
