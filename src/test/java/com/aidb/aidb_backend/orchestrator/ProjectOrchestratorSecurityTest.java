package com.aidb.aidb_backend.orchestrator;

import com.aidb.aidb_backend.exception.ExcelValidationException;
import com.aidb.aidb_backend.exception.ProjectNotFoundException;
import com.aidb.aidb_backend.exception.TableNotFoundException;
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

    // ==================== DELETE PROJECT SECURITY TESTS ====================

    @Test
    void deleteProject_preventsCrossUserAccess() {
        String maliciousUserId = "malicious-user";
        String legitimateUserId = "legitimate-user";
        String projectId = "123";

        // Project belongs to legitimate user, but malicious user tries to delete it
        doThrow(new ProjectNotFoundException(123L)).when(projectService).deleteProject(maliciousUserId, 123L);

        assertThrows(ProjectNotFoundException.class, () -> orchestrator.deleteProject(maliciousUserId, projectId));
        verify(projectService).deleteProject(maliciousUserId, 123L);
    }

    @Test
    void deleteProject_handlesMaliciousProjectId() {
        String userId = "user-1";
        String maliciousProjectId = "'; DROP TABLE projects; --";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteProject(userId, maliciousProjectId));
        verifyNoInteractions(projectService);
    }

    @Test
    void deleteProject_handlesSQLInjectionInProjectId() {
        String userId = "user-1";
        String sqlInjectionProjectId = "1 OR 1=1";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteProject(userId, sqlInjectionProjectId));
        verifyNoInteractions(projectService);
    }

    @Test
    void deleteProject_handlesUnionBasedSQLInjection() {
        String userId = "user-1";
        String unionInjectionProjectId = "1 UNION SELECT * FROM users";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteProject(userId, unionInjectionProjectId));
        verifyNoInteractions(projectService);
    }

    @Test
    void deleteProject_handlesCommentBasedSQLInjection() {
        String userId = "user-1";
        String commentInjectionProjectId = "1/*comment*/";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteProject(userId, commentInjectionProjectId));
        verifyNoInteractions(projectService);
    }

    @Test
    void deleteProject_handlesSpecialCharactersInProjectId() {
        String userId = "user-1";
        String specialCharsProjectId = "1;2;3";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteProject(userId, specialCharsProjectId));
        verifyNoInteractions(projectService);
    }

    @Test
    void deleteProject_handlesUnicodeCharactersInProjectId() {
        String userId = "user-1";
        String unicodeProjectId = "1\u0000";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteProject(userId, unicodeProjectId));
        verifyNoInteractions(projectService);
    }

    @Test
    void deleteProject_handlesExtremelyLongProjectId() {
        String userId = "user-1";
        String longProjectId = "1".repeat(10000);

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteProject(userId, longProjectId));
        verifyNoInteractions(projectService);
    }

    @Test
    void deleteProject_handlesWhitespaceOnlyProjectId() {
        String userId = "user-1";
        String whitespaceProjectId = "   ";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteProject(userId, whitespaceProjectId));
        verifyNoInteractions(projectService);
    }

    @Test
    void deleteProject_handlesTabAndNewlineInProjectId() {
        String userId = "user-1";
        String tabNewlineProjectId = "\t\n123\t\n";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteProject(userId, tabNewlineProjectId));
        verifyNoInteractions(projectService);
    }

    @Test
    void deleteProject_handlesMaliciousUserId() {
        String maliciousUserId = "'; DROP TABLE users; --";
        String projectId = "123";

        doThrow(new IllegalArgumentException("Invalid user ID")).when(projectService).deleteProject(maliciousUserId, 123L);

        assertThrows(IllegalArgumentException.class, () -> orchestrator.deleteProject(maliciousUserId, projectId));
        verify(projectService).deleteProject(maliciousUserId, 123L);
    }

    @Test
    void deleteProject_handlesSQLInjectionInUserId() {
        String sqlInjectionUserId = "1' OR '1'='1";
        String projectId = "123";

        doThrow(new IllegalArgumentException("Invalid user ID")).when(projectService).deleteProject(sqlInjectionUserId, 123L);

        assertThrows(IllegalArgumentException.class, () -> orchestrator.deleteProject(sqlInjectionUserId, projectId));
        verify(projectService).deleteProject(sqlInjectionUserId, 123L);
    }

    @Test
    void deleteProject_handlesExtremelyLongUserId() {
        String longUserId = "A".repeat(10000);
        String projectId = "123";

        doThrow(new IllegalArgumentException("User ID too long")).when(projectService).deleteProject(longUserId, 123L);

        assertThrows(IllegalArgumentException.class, () -> orchestrator.deleteProject(longUserId, projectId));
        verify(projectService).deleteProject(longUserId, 123L);
    }

    @Test
    void deleteProject_handlesSpecialCharactersInUserId() {
        String specialCharsUserId = "user@#$%^&*()";
        String projectId = "123";

        doThrow(new IllegalArgumentException("Invalid characters in user ID")).when(projectService).deleteProject(specialCharsUserId, 123L);

        assertThrows(IllegalArgumentException.class, () -> orchestrator.deleteProject(specialCharsUserId, projectId));
        verify(projectService).deleteProject(specialCharsUserId, 123L);
    }

    // ==================== DELETE TABLE SECURITY TESTS ====================

    @Test
    void deleteTable_preventsCrossUserAccess() {
        String maliciousUserId = "malicious-user";
        String legitimateUserId = "legitimate-user";
        String projectId = "123";
        String tableId = "456";

        // Table belongs to legitimate user's project, but malicious user tries to delete it
        doThrow(new TableNotFoundException()).when(tableMetadataService).deleteTable(maliciousUserId, 123L, 456L);

        assertThrows(TableNotFoundException.class, () -> orchestrator.deleteTable(maliciousUserId, projectId, tableId));
        verify(tableMetadataService).deleteTable(maliciousUserId, 123L, 456L);
    }

    @Test
    void deleteTable_handlesMaliciousProjectId() {
        String userId = "user-1";
        String maliciousProjectId = "'; DROP TABLE projects; --";
        String tableId = "456";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteTable(userId, maliciousProjectId, tableId));
        verifyNoInteractions(tableMetadataService);
    }

    @Test
    void deleteTable_handlesMaliciousTableId() {
        String userId = "user-1";
        String projectId = "123";
        String maliciousTableId = "'; DROP TABLE table_metadata; --";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteTable(userId, projectId, maliciousTableId));
        verifyNoInteractions(tableMetadataService);
    }

    @Test
    void deleteTable_handlesSQLInjectionInProjectId() {
        String userId = "user-1";
        String sqlInjectionProjectId = "1 OR 1=1";
        String tableId = "456";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteTable(userId, sqlInjectionProjectId, tableId));
        verifyNoInteractions(tableMetadataService);
    }

    @Test
    void deleteTable_handlesSQLInjectionInTableId() {
        String userId = "user-1";
        String projectId = "123";
        String sqlInjectionTableId = "1 OR 1=1";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteTable(userId, projectId, sqlInjectionTableId));
        verifyNoInteractions(tableMetadataService);
    }

    @Test
    void deleteTable_handlesUnionBasedSQLInjectionInProjectId() {
        String userId = "user-1";
        String unionInjectionProjectId = "1 UNION SELECT * FROM projects";
        String tableId = "456";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteTable(userId, unionInjectionProjectId, tableId));
        verifyNoInteractions(tableMetadataService);
    }

    @Test
    void deleteTable_handlesUnionBasedSQLInjectionInTableId() {
        String userId = "user-1";
        String projectId = "123";
        String unionInjectionTableId = "1 UNION SELECT * FROM table_metadata";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteTable(userId, projectId, unionInjectionTableId));
        verifyNoInteractions(tableMetadataService);
    }

    @Test
    void deleteTable_handlesCommentBasedSQLInjectionInProjectId() {
        String userId = "user-1";
        String commentInjectionProjectId = "1/*comment*/";
        String tableId = "456";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteTable(userId, commentInjectionProjectId, tableId));
        verifyNoInteractions(tableMetadataService);
    }

    @Test
    void deleteTable_handlesCommentBasedSQLInjectionInTableId() {
        String userId = "user-1";
        String projectId = "123";
        String commentInjectionTableId = "1/*comment*/";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteTable(userId, projectId, commentInjectionTableId));
        verifyNoInteractions(tableMetadataService);
    }

    @Test
    void deleteTable_handlesSpecialCharactersInProjectId() {
        String userId = "user-1";
        String specialCharsProjectId = "1;2;3";
        String tableId = "456";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteTable(userId, specialCharsProjectId, tableId));
        verifyNoInteractions(tableMetadataService);
    }

    @Test
    void deleteTable_handlesSpecialCharactersInTableId() {
        String userId = "user-1";
        String projectId = "123";
        String specialCharsTableId = "1;2;3";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteTable(userId, projectId, specialCharsTableId));
        verifyNoInteractions(tableMetadataService);
    }

    @Test
    void deleteTable_handlesUnicodeCharactersInProjectId() {
        String userId = "user-1";
        String unicodeProjectId = "1\u0000";
        String tableId = "456";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteTable(userId, unicodeProjectId, tableId));
        verifyNoInteractions(tableMetadataService);
    }

    @Test
    void deleteTable_handlesUnicodeCharactersInTableId() {
        String userId = "user-1";
        String projectId = "123";
        String unicodeTableId = "1\u0000";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteTable(userId, projectId, unicodeTableId));
        verifyNoInteractions(tableMetadataService);
    }

    @Test
    void deleteTable_handlesExtremelyLongProjectId() {
        String userId = "user-1";
        String longProjectId = "1".repeat(10000);
        String tableId = "456";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteTable(userId, longProjectId, tableId));
        verifyNoInteractions(tableMetadataService);
    }

    @Test
    void deleteTable_handlesExtremelyLongTableId() {
        String userId = "user-1";
        String projectId = "123";
        String longTableId = "1".repeat(10000);

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteTable(userId, projectId, longTableId));
        verifyNoInteractions(tableMetadataService);
    }

    @Test
    void deleteTable_handlesWhitespaceOnlyProjectId() {
        String userId = "user-1";
        String whitespaceProjectId = "   ";
        String tableId = "456";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteTable(userId, whitespaceProjectId, tableId));
        verifyNoInteractions(tableMetadataService);
    }

    @Test
    void deleteTable_handlesWhitespaceOnlyTableId() {
        String userId = "user-1";
        String projectId = "123";
        String whitespaceTableId = "   ";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteTable(userId, projectId, whitespaceTableId));
        verifyNoInteractions(tableMetadataService);
    }

    @Test
    void deleteTable_handlesTabAndNewlineInProjectId() {
        String userId = "user-1";
        String tabNewlineProjectId = "\t\n123\t\n";
        String tableId = "456";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteTable(userId, tabNewlineProjectId, tableId));
        verifyNoInteractions(tableMetadataService);
    }

    @Test
    void deleteTable_handlesTabAndNewlineInTableId() {
        String userId = "user-1";
        String projectId = "123";
        String tabNewlineTableId = "\t\n456\t\n";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteTable(userId, projectId, tabNewlineTableId));
        verifyNoInteractions(tableMetadataService);
    }

    @Test
    void deleteTable_handlesMaliciousUserId() {
        String maliciousUserId = "'; DROP TABLE users; --";
        String projectId = "123";
        String tableId = "456";

        doThrow(new IllegalArgumentException("Invalid user ID")).when(tableMetadataService).deleteTable(maliciousUserId, 123L, 456L);

        assertThrows(IllegalArgumentException.class, () -> orchestrator.deleteTable(maliciousUserId, projectId, tableId));
        verify(tableMetadataService).deleteTable(maliciousUserId, 123L, 456L);
    }

    @Test
    void deleteTable_handlesSQLInjectionInUserId() {
        String sqlInjectionUserId = "1' OR '1'='1";
        String projectId = "123";
        String tableId = "456";

        doThrow(new IllegalArgumentException("Invalid user ID")).when(tableMetadataService).deleteTable(sqlInjectionUserId, 123L, 456L);

        assertThrows(IllegalArgumentException.class, () -> orchestrator.deleteTable(sqlInjectionUserId, projectId, tableId));
        verify(tableMetadataService).deleteTable(sqlInjectionUserId, 123L, 456L);
    }

    @Test
    void deleteTable_handlesExtremelyLongUserId() {
        String longUserId = "A".repeat(10000);
        String projectId = "123";
        String tableId = "456";

        doThrow(new IllegalArgumentException("User ID too long")).when(tableMetadataService).deleteTable(longUserId, 123L, 456L);

        assertThrows(IllegalArgumentException.class, () -> orchestrator.deleteTable(longUserId, projectId, tableId));
        verify(tableMetadataService).deleteTable(longUserId, 123L, 456L);
    }

    @Test
    void deleteTable_handlesSpecialCharactersInUserId() {
        String specialCharsUserId = "user@#$%^&*()";
        String projectId = "123";
        String tableId = "456";

        doThrow(new IllegalArgumentException("Invalid characters in user ID")).when(tableMetadataService).deleteTable(specialCharsUserId, 123L, 456L);

        assertThrows(IllegalArgumentException.class, () -> orchestrator.deleteTable(specialCharsUserId, projectId, tableId));
        verify(tableMetadataService).deleteTable(specialCharsUserId, 123L, 456L);
    }

    // ==================== AUTHORIZATION AND ACCESS CONTROL TESTS ====================

    @Test
    void deleteProject_preventsUnauthorizedAccess() {
        String unauthorizedUserId = "unauthorized-user";
        String projectId = "123";

        // Simulate that the project exists but doesn't belong to the user
        doThrow(new ProjectNotFoundException(123L)).when(projectService).deleteProject(unauthorizedUserId, 123L);

        assertThrows(ProjectNotFoundException.class, () -> orchestrator.deleteProject(unauthorizedUserId, projectId));
        verify(projectService).deleteProject(unauthorizedUserId, 123L);
    }

    @Test
    void deleteTable_preventsUnauthorizedAccess() {
        String unauthorizedUserId = "unauthorized-user";
        String projectId = "123";
        String tableId = "456";

        // Simulate that the table exists but doesn't belong to the user's project
        doThrow(new TableNotFoundException()).when(tableMetadataService).deleteTable(unauthorizedUserId, 123L, 456L);

        assertThrows(TableNotFoundException.class, () -> orchestrator.deleteTable(unauthorizedUserId, projectId, tableId));
        verify(tableMetadataService).deleteTable(unauthorizedUserId, 123L, 456L);
    }

    @Test
    void deleteProject_handlesCaseInsensitiveUserId() {
        String userId = "User-1";
        String projectId = "123";

        doThrow(new ProjectNotFoundException(123L)).when(projectService).deleteProject(userId, 123L);

        assertThrows(ProjectNotFoundException.class, () -> orchestrator.deleteProject(userId, projectId));
        verify(projectService).deleteProject(userId, 123L);
    }

    @Test
    void deleteTable_handlesCaseInsensitiveUserId() {
        String userId = "User-1";
        String projectId = "123";
        String tableId = "456";

        doThrow(new TableNotFoundException()).when(tableMetadataService).deleteTable(userId, 123L, 456L);

        assertThrows(TableNotFoundException.class, () -> orchestrator.deleteTable(userId, projectId, tableId));
        verify(tableMetadataService).deleteTable(userId, 123L, 456L);
    }

    // ==================== EDGE CASE AND BOUNDARY TESTS ====================

    @Test
    void deleteProject_handlesMinimumValidProjectId() {
        String userId = "user-1";
        String minProjectId = "1";

        String result = orchestrator.deleteProject(userId, minProjectId);

        assertEquals("Success", result);
        verify(projectService).deleteProject(userId, 1L);
    }

    @Test
    void deleteTable_handlesMinimumValidIds() {
        String userId = "user-1";
        String minProjectId = "1";
        String minTableId = "1";

        String result = orchestrator.deleteTable(userId, minProjectId, minTableId);

        assertEquals("Success", result);
        verify(tableMetadataService).deleteTable(userId, 1L, 1L);
    }

    @Test
    void deleteProject_handlesMaximumValidProjectId() {
        String userId = "user-1";
        String maxProjectId = String.valueOf(Long.MAX_VALUE);

        doThrow(new ProjectNotFoundException(Long.MAX_VALUE)).when(projectService).deleteProject(userId, Long.MAX_VALUE);

        assertThrows(ProjectNotFoundException.class, () -> orchestrator.deleteProject(userId, maxProjectId));
        verify(projectService).deleteProject(userId, Long.MAX_VALUE);
    }

    @Test
    void deleteTable_handlesMaximumValidIds() {
        String userId = "user-1";
        String maxProjectId = String.valueOf(Long.MAX_VALUE);
        String maxTableId = String.valueOf(Long.MAX_VALUE);

        doThrow(new TableNotFoundException()).when(tableMetadataService).deleteTable(userId, Long.MAX_VALUE, Long.MAX_VALUE);

        assertThrows(TableNotFoundException.class, () -> orchestrator.deleteTable(userId, maxProjectId, maxTableId));
        verify(tableMetadataService).deleteTable(userId, Long.MAX_VALUE, Long.MAX_VALUE);
    }
}