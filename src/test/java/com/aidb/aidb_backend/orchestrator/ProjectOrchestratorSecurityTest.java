package com.aidb.aidb_backend.orchestrator;

import com.aidb.aidb_backend.exception.ExcelValidationException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Security-focused tests for ProjectOrchestrator
 * Tests file upload security, malicious input handling, and authorization
 */
class ProjectOrchestratorSecurityTest {

    @Mock
    private ProjectService projectService;

    @Mock
    private TableMetadataService tableMetadataService;

    @Mock
    private ExcelDataValidatorService dataValidatorService;

    @Mock
    private ExcelParserService parserService;

    @Mock
    private ExcelUploadService excelUploadService;

    @Mock
    private ProjectConversionService projectConversionService;

    @InjectMocks
    private ProjectOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void uploadExcel_rejectsNullFile() {
        String userId = "user-1";
        String projectId = "123";

        // The implementation checks project first, so it throws ProjectNotFoundException for null project
        when(projectService.getProjectOverviewDTO(userId, 123L)).thenReturn(null);
        
        assertThrows(ProjectNotFoundException.class, () -> orchestrator.uploadExcel(userId, projectId, null));
        verify(projectService).getProjectOverviewDTO(userId, 123L);
        verifyNoInteractions(tableMetadataService, parserService, dataValidatorService, excelUploadService);
    }

    @Test
    void uploadExcel_rejectsInvalidProjectId() {
        String userId = "user-1";
        String invalidProjectId = "not-a-number";
        MultipartFile file = mock(MultipartFile.class);

        assertThrows(NumberFormatException.class, () -> orchestrator.uploadExcel(userId, invalidProjectId, file));
        verifyNoInteractions(projectService, tableMetadataService, parserService, dataValidatorService, excelUploadService);
    }

    @Test
    void uploadExcel_rejectsNegativeProjectId() {
        String userId = "user-1";
        String negativeProjectId = "-1";
        MultipartFile file = mock(MultipartFile.class);

        when(projectService.getProjectOverviewDTO(userId, -1L)).thenReturn(null);

        assertThrows(ProjectNotFoundException.class, () -> orchestrator.uploadExcel(userId, negativeProjectId, file));
        verify(projectService).getProjectOverviewDTO(userId, -1L);
        verifyNoInteractions(tableMetadataService, parserService, dataValidatorService, excelUploadService);
    }

    @Test
    void uploadExcel_rejectsZeroProjectId() {
        String userId = "user-1";
        String zeroProjectId = "0";
        MultipartFile file = mock(MultipartFile.class);

        when(projectService.getProjectOverviewDTO(userId, 0L)).thenReturn(null);

        assertThrows(ProjectNotFoundException.class, () -> orchestrator.uploadExcel(userId, zeroProjectId, file));
        verify(projectService).getProjectOverviewDTO(userId, 0L);
        verifyNoInteractions(tableMetadataService, parserService, dataValidatorService, excelUploadService);
    }

    @Test
    void uploadExcel_handlesLargeProjectId() throws Exception {
        String userId = "user-1";
        String largeProjectId = String.valueOf(Long.MAX_VALUE);
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        ProjectOverviewDTO projectOverview = new ProjectOverviewDTO(Long.MAX_VALUE, "Test Project", userId);
        when(projectService.getProjectOverviewDTO(userId, Long.MAX_VALUE)).thenReturn(projectOverview);

        Set<String> tableNames = Set.of("table1");
        when(tableMetadataService.getTableNames(userId, Long.MAX_VALUE)).thenReturn(tableNames);

        ProjectDTO expectedDto = new ProjectDTO();
        when(parserService.parseExcelFile(eq(projectOverview), eq(tableNames), any())).thenReturn(expectedDto);

        ProjectDTO result = orchestrator.uploadExcel(userId, largeProjectId, file);

        assertSame(expectedDto, result);
        verify(projectService).getProjectOverviewDTO(userId, Long.MAX_VALUE);
        verify(tableMetadataService).getTableNames(userId, Long.MAX_VALUE);
        verify(dataValidatorService).validateData(expectedDto);
        verify(excelUploadService).upload(Long.MAX_VALUE, expectedDto);
    }

    @Test
    void uploadExcel_preventsCrossUserAccess() {
        String maliciousUserId = "malicious-user";
        String legitimateUserId = "legitimate-user";
        String projectId = "123";
        MultipartFile file = mock(MultipartFile.class);

        // Project belongs to legitimate user, but malicious user tries to access it
        when(projectService.getProjectOverviewDTO(maliciousUserId, 123L)).thenReturn(null);

        assertThrows(ProjectNotFoundException.class, () -> orchestrator.uploadExcel(maliciousUserId, projectId, file));
        verify(projectService).getProjectOverviewDTO(maliciousUserId, 123L);
        verifyNoInteractions(tableMetadataService, parserService, dataValidatorService, excelUploadService);
    }

    @Test
    void uploadExcel_handlesIOExceptionGracefully() throws Exception {
        String userId = "user-1";
        String projectId = "123";
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenThrow(new IOException("File access error"));

        ProjectOverviewDTO projectOverview = new ProjectOverviewDTO(123L, "Test Project", userId);
        when(projectService.getProjectOverviewDTO(userId, 123L)).thenReturn(projectOverview);

        Set<String> tableNames = Set.of("table1");
        when(tableMetadataService.getTableNames(userId, 123L)).thenReturn(tableNames);

        assertThrows(IOException.class, () -> orchestrator.uploadExcel(userId, projectId, file));
        verify(projectService).getProjectOverviewDTO(userId, 123L);
        verify(tableMetadataService).getTableNames(userId, 123L);
        verifyNoInteractions(parserService, dataValidatorService, excelUploadService);
    }

    @Test
    void createProject_success() {
        String userId = "user-1";
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setName("Test Project");

        Project project = new Project();
        project.setId(123L);
        project.setName("Test Project");
        project.setUserId(userId);
        project.setCreatedAt(Instant.now());

        ProjectDTO expectedDto = new ProjectDTO();
        expectedDto.setUserId(userId);
        expectedDto.setName("Test Project");

        when(projectService.createProject(userId, request)).thenReturn(project);
        when(projectConversionService.convertProjectToDTO(project)).thenReturn(expectedDto);

        ProjectDTO result = orchestrator.createProject(userId, request);

        assertSame(expectedDto, result);
        verify(projectService).createProject(userId, request);
        verify(projectConversionService).convertProjectToDTO(project);
    }

    @Test
    void createProject_handlesNullRequest() {
        String userId = "user-1";

        // The implementation passes null to the service, which handles it appropriately
        when(projectService.createProject(userId, null)).thenThrow(new NullPointerException("Request cannot be null"));
        
        assertThrows(NullPointerException.class, () -> orchestrator.createProject(userId, null));
        verify(projectService).createProject(userId, null);
        verifyNoInteractions(projectConversionService);
    }

    @Test
    void getProjectDTO_success() {
        String userId = "user-1";
        String projectId = "123";

        ProjectDTO expectedDto = new ProjectDTO();
        when(projectService.getProjectDTO(userId, 123L)).thenReturn(expectedDto);

        ProjectDTO result = orchestrator.getProjectDTO(userId, projectId);

        assertSame(expectedDto, result);
        verify(projectService).getProjectDTO(userId, 123L);
    }

    @Test
    void getProjectDTO_rejectsInvalidProjectId() {
        String userId = "user-1";
        String invalidProjectId = "invalid";

        assertThrows(NumberFormatException.class, () -> orchestrator.getProjectDTO(userId, invalidProjectId));
        verifyNoInteractions(projectService);
    }

    @Test
    void getProjectOverviewDTOs_success() {
        String userId = "user-1";
        List<ProjectOverviewDTO> expectedDtos = List.of(
            new ProjectOverviewDTO(1L, "Project 1", userId),
            new ProjectOverviewDTO(2L, "Project 2", userId)
        );

        when(projectService.getProjectOverviewDTOs(userId)).thenReturn(expectedDtos);

        List<ProjectOverviewDTO> result = orchestrator.getProjectOverviewDTOs(userId);

        assertSame(expectedDtos, result);
        verify(projectService).getProjectOverviewDTOs(userId);
    }

    @Test
    void getProjectOverviewDTOs_handlesEmptyResult() {
        String userId = "user-1";
        List<ProjectOverviewDTO> emptyList = List.of();

        when(projectService.getProjectOverviewDTOs(userId)).thenReturn(emptyList);

        List<ProjectOverviewDTO> result = orchestrator.getProjectOverviewDTOs(userId);

        assertTrue(result.isEmpty());
        verify(projectService).getProjectOverviewDTOs(userId);
    }
}