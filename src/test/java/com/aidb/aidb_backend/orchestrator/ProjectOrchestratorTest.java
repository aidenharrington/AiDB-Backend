package com.aidb.aidb_backend.orchestrator;

import com.aidb.aidb_backend.exception.ExcelValidationException;
import com.aidb.aidb_backend.exception.ProjectNotFoundException;
import com.aidb.aidb_backend.exception.TableNotFoundException;
import com.aidb.aidb_backend.model.dto.ProjectDTO;
import com.aidb.aidb_backend.model.dto.ProjectOverviewDTO;
import com.aidb.aidb_backend.service.database.postgres.user_created_tables.ExcelUploadService;
import com.aidb.aidb_backend.service.database.postgres.ProjectService;
import com.aidb.aidb_backend.service.database.postgres.TableMetadataService;
import com.aidb.aidb_backend.service.util.excel.ExcelDataValidatorService;
import com.aidb.aidb_backend.service.util.excel.ExcelParserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProjectOrchestratorTest {

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

    @InjectMocks
    private ProjectOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void uploadExcel_success_callsServicesInOrder_andReturnsDto() throws Exception {
        String userId = "user-1";
        Long projectId = 42L;
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        ProjectOverviewDTO projectOverview = new ProjectOverviewDTO(projectId, "Test Project", userId);
        when(projectService.getProjectOverviewDTO(userId, projectId)).thenReturn(projectOverview);

        Set<String> tableNames = Set.of("table1", "table2");
        when(tableMetadataService.getTableNames(userId, projectId)).thenReturn(tableNames);

        ProjectDTO expectedDto = new ProjectDTO();
        when(parserService.parseExcelFile(eq(projectOverview), eq(tableNames), any())).thenReturn(expectedDto);

        ProjectDTO result = orchestrator.uploadExcel(userId, String.valueOf(projectId), file);

        assertSame(expectedDto, result);
        verify(projectService).getProjectOverviewDTO(userId, projectId);
        verify(tableMetadataService).getTableNames(userId, projectId);
        verify(parserService).parseExcelFile(eq(projectOverview), eq(tableNames), any());
        verify(dataValidatorService).validateData(expectedDto);
        verify(excelUploadService).upload(projectId, expectedDto);
    }

    @Test
    void uploadExcel_propagatesIOExceptionFromParser() throws Exception {
        String userId = "user-1";
        Long projectId = 1L;
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        ProjectOverviewDTO projectOverview = new ProjectOverviewDTO(projectId, "Test Project", userId);
        when(projectService.getProjectOverviewDTO(userId, projectId)).thenReturn(projectOverview);

        Set<String> tableNames = Set.of("table1");
        when(tableMetadataService.getTableNames(userId, projectId)).thenReturn(tableNames);

        when(parserService.parseExcelFile(any(), any(), any())).thenThrow(new IOException("bad file"));

        assertThrows(IOException.class, () -> orchestrator.uploadExcel(userId, String.valueOf(projectId), file));
        verify(dataValidatorService, never()).validateData(any());
        verify(excelUploadService, never()).upload(anyLong(), any());
    }

    @Test
    void uploadExcel_propagatesValidationException() throws Exception {
        String userId = "user-1";
        Long projectId = 1L;
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        ProjectOverviewDTO projectOverview = new ProjectOverviewDTO(projectId, "Test Project", userId);
        when(projectService.getProjectOverviewDTO(userId, projectId)).thenReturn(projectOverview);

        Set<String> tableNames = Set.of("table1");
        when(tableMetadataService.getTableNames(userId, projectId)).thenReturn(tableNames);

        ProjectDTO projectData = new ProjectDTO();
        when(parserService.parseExcelFile(any(), any(), any())).thenReturn(projectData);
        doThrow(new ExcelValidationException("invalid", null)).when(dataValidatorService).validateData(projectData);

        assertThrows(ExcelValidationException.class, () -> orchestrator.uploadExcel(userId, String.valueOf(projectId), file));
        verify(excelUploadService, never()).upload(anyLong(), any());
    }

    @Test
    void uploadExcel_throwsProjectNotFoundExceptionWhenProjectOverviewIsNull() throws Exception {
        String userId = "user-1";
        Long projectId = 1L;
        MultipartFile file = mock(MultipartFile.class);

        when(projectService.getProjectOverviewDTO(userId, projectId)).thenReturn(null);

        assertThrows(ProjectNotFoundException.class, () -> orchestrator.uploadExcel(userId, String.valueOf(projectId), file));
        verify(tableMetadataService, never()).getTableNames(anyString(), anyLong());
        verify(parserService, never()).parseExcelFile(any(), any(), any());
        verify(dataValidatorService, never()).validateData(any());
        verify(excelUploadService, never()).upload(anyLong(), any());
    }

    // ==================== DELETE PROJECT TESTS ====================

    @Test
    void deleteProject_success_returnsSuccessResponse() {
        String userId = "user-1";
        String projectId = "123";

        String result = orchestrator.deleteProject(userId, projectId);

        assertEquals("Success", result);
        verify(projectService).deleteProject(userId, 123L);
    }

    @Test
    void deleteProject_propagatesProjectNotFoundException() {
        String userId = "user-1";
        String projectId = "123";

        doThrow(new ProjectNotFoundException(123L)).when(projectService).deleteProject(userId, 123L);

        assertThrows(ProjectNotFoundException.class, () -> orchestrator.deleteProject(userId, projectId));
        verify(projectService).deleteProject(userId, 123L);
    }

    @Test
    void deleteProject_handlesInvalidProjectId() {
        String userId = "user-1";
        String invalidProjectId = "not-a-number";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteProject(userId, invalidProjectId));
        verifyNoInteractions(projectService);
    }

    @Test
    void deleteProject_handlesNegativeProjectId() {
        String userId = "user-1";
        String negativeProjectId = "-1";

        doThrow(new ProjectNotFoundException(-1L)).when(projectService).deleteProject(userId, -1L);

        assertThrows(ProjectNotFoundException.class, () -> orchestrator.deleteProject(userId, negativeProjectId));
        verify(projectService).deleteProject(userId, -1L);
    }

    @Test
    void deleteProject_handlesZeroProjectId() {
        String userId = "user-1";
        String zeroProjectId = "0";

        doThrow(new ProjectNotFoundException(0L)).when(projectService).deleteProject(userId, 0L);

        assertThrows(ProjectNotFoundException.class, () -> orchestrator.deleteProject(userId, zeroProjectId));
        verify(projectService).deleteProject(userId, 0L);
    }

    @Test
    void deleteProject_handlesLargeProjectId() {
        String userId = "user-1";
        String largeProjectId = String.valueOf(Long.MAX_VALUE);

        doThrow(new ProjectNotFoundException(Long.MAX_VALUE)).when(projectService).deleteProject(userId, Long.MAX_VALUE);

        assertThrows(ProjectNotFoundException.class, () -> orchestrator.deleteProject(userId, largeProjectId));
        verify(projectService).deleteProject(userId, Long.MAX_VALUE);
    }

    @Test
    void deleteProject_handlesNullUserId() {
        String projectId = "123";

        doThrow(new IllegalArgumentException("User ID cannot be null")).when(projectService).deleteProject(null, 123L);

        assertThrows(IllegalArgumentException.class, () -> orchestrator.deleteProject(null, projectId));
        verify(projectService).deleteProject(null, 123L);
    }

    @Test
    void deleteProject_handlesEmptyUserId() {
        String userId = "";
        String projectId = "123";

        doThrow(new IllegalArgumentException("User ID cannot be empty")).when(projectService).deleteProject("", 123L);

        assertThrows(IllegalArgumentException.class, () -> orchestrator.deleteProject(userId, projectId));
        verify(projectService).deleteProject("", 123L);
    }

    @Test
    void deleteProject_handlesNullProjectId() {
        String userId = "user-1";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteProject(userId, null));
        verifyNoInteractions(projectService);
    }

    @Test
    void deleteProject_handlesEmptyProjectId() {
        String userId = "user-1";
        String emptyProjectId = "";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteProject(userId, emptyProjectId));
        verifyNoInteractions(projectService);
    }

    // ==================== DELETE TABLE TESTS ====================

    @Test
    void deleteTable_success_returnsSuccessResponse() {
        String userId = "user-1";
        String projectId = "123";
        String tableId = "456";

        String result = orchestrator.deleteTable(userId, projectId, tableId);

        assertEquals("Success", result);
        verify(tableMetadataService).deleteTable(userId, 123L, 456L);
    }

    @Test
    void deleteTable_propagatesTableNotFoundException() {
        String userId = "user-1";
        String projectId = "123";
        String tableId = "456";

        doThrow(new TableNotFoundException()).when(tableMetadataService).deleteTable(userId, 123L, 456L);

        assertThrows(TableNotFoundException.class, () -> orchestrator.deleteTable(userId, projectId, tableId));
        verify(tableMetadataService).deleteTable(userId, 123L, 456L);
    }

    @Test
    void deleteTable_handlesInvalidProjectId() {
        String userId = "user-1";
        String invalidProjectId = "not-a-number";
        String tableId = "456";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteTable(userId, invalidProjectId, tableId));
        verifyNoInteractions(tableMetadataService);
    }

    @Test
    void deleteTable_handlesInvalidTableId() {
        String userId = "user-1";
        String projectId = "123";
        String invalidTableId = "not-a-number";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteTable(userId, projectId, invalidTableId));
        verifyNoInteractions(tableMetadataService);
    }

    @Test
    void deleteTable_handlesNegativeProjectId() {
        String userId = "user-1";
        String negativeProjectId = "-1";
        String tableId = "456";

        doThrow(new TableNotFoundException()).when(tableMetadataService).deleteTable(userId, -1L, 456L);

        assertThrows(TableNotFoundException.class, () -> orchestrator.deleteTable(userId, negativeProjectId, tableId));
        verify(tableMetadataService).deleteTable(userId, -1L, 456L);
    }

    @Test
    void deleteTable_handlesNegativeTableId() {
        String userId = "user-1";
        String projectId = "123";
        String negativeTableId = "-1";

        doThrow(new TableNotFoundException()).when(tableMetadataService).deleteTable(userId, 123L, -1L);

        assertThrows(TableNotFoundException.class, () -> orchestrator.deleteTable(userId, projectId, negativeTableId));
        verify(tableMetadataService).deleteTable(userId, 123L, -1L);
    }

    @Test
    void deleteTable_handlesZeroProjectId() {
        String userId = "user-1";
        String zeroProjectId = "0";
        String tableId = "456";

        doThrow(new TableNotFoundException()).when(tableMetadataService).deleteTable(userId, 0L, 456L);

        assertThrows(TableNotFoundException.class, () -> orchestrator.deleteTable(userId, zeroProjectId, tableId));
        verify(tableMetadataService).deleteTable(userId, 0L, 456L);
    }

    @Test
    void deleteTable_handlesZeroTableId() {
        String userId = "user-1";
        String projectId = "123";
        String zeroTableId = "0";

        doThrow(new TableNotFoundException()).when(tableMetadataService).deleteTable(userId, 123L, 0L);

        assertThrows(TableNotFoundException.class, () -> orchestrator.deleteTable(userId, projectId, zeroTableId));
        verify(tableMetadataService).deleteTable(userId, 123L, 0L);
    }

    @Test
    void deleteTable_handlesLargeProjectId() {
        String userId = "user-1";
        String largeProjectId = String.valueOf(Long.MAX_VALUE);
        String tableId = "456";

        doThrow(new TableNotFoundException()).when(tableMetadataService).deleteTable(userId, Long.MAX_VALUE, 456L);

        assertThrows(TableNotFoundException.class, () -> orchestrator.deleteTable(userId, largeProjectId, tableId));
        verify(tableMetadataService).deleteTable(userId, Long.MAX_VALUE, 456L);
    }

    @Test
    void deleteTable_handlesLargeTableId() {
        String userId = "user-1";
        String projectId = "123";
        String largeTableId = String.valueOf(Long.MAX_VALUE);

        doThrow(new TableNotFoundException()).when(tableMetadataService).deleteTable(userId, 123L, Long.MAX_VALUE);

        assertThrows(TableNotFoundException.class, () -> orchestrator.deleteTable(userId, projectId, largeTableId));
        verify(tableMetadataService).deleteTable(userId, 123L, Long.MAX_VALUE);
    }

    @Test
    void deleteTable_handlesNullUserId() {
        String projectId = "123";
        String tableId = "456";

        doThrow(new IllegalArgumentException("User ID cannot be null")).when(tableMetadataService).deleteTable(null, 123L, 456L);

        assertThrows(IllegalArgumentException.class, () -> orchestrator.deleteTable(null, projectId, tableId));
        verify(tableMetadataService).deleteTable(null, 123L, 456L);
    }

    @Test
    void deleteTable_handlesEmptyUserId() {
        String userId = "";
        String projectId = "123";
        String tableId = "456";

        doThrow(new IllegalArgumentException("User ID cannot be empty")).when(tableMetadataService).deleteTable("", 123L, 456L);

        assertThrows(IllegalArgumentException.class, () -> orchestrator.deleteTable(userId, projectId, tableId));
        verify(tableMetadataService).deleteTable("", 123L, 456L);
    }

    @Test
    void deleteTable_handlesNullProjectId() {
        String userId = "user-1";
        String tableId = "456";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteTable(userId, null, tableId));
        verifyNoInteractions(tableMetadataService);
    }

    @Test
    void deleteTable_handlesNullTableId() {
        String userId = "user-1";
        String projectId = "123";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteTable(userId, projectId, null));
        verifyNoInteractions(tableMetadataService);
    }

    @Test
    void deleteTable_handlesEmptyProjectId() {
        String userId = "user-1";
        String emptyProjectId = "";
        String tableId = "456";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteTable(userId, emptyProjectId, tableId));
        verifyNoInteractions(tableMetadataService);
    }

    @Test
    void deleteTable_handlesEmptyTableId() {
        String userId = "user-1";
        String projectId = "123";
        String emptyTableId = "";

        assertThrows(NumberFormatException.class, () -> orchestrator.deleteTable(userId, projectId, emptyTableId));
        verifyNoInteractions(tableMetadataService);
    }
}