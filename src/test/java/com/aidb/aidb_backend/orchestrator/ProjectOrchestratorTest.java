package com.aidb.aidb_backend.orchestrator;

import com.aidb.aidb_backend.exception.ExcelValidationException;
import com.aidb.aidb_backend.exception.ProjectNotFoundException;
import com.aidb.aidb_backend.model.dto.ProjectDTO;
import com.aidb.aidb_backend.model.dto.ProjectOverviewDTO;
import com.aidb.aidb_backend.service.database.postgres.ExcelUploadService;
import com.aidb.aidb_backend.service.database.postgres.ProjectService;
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
        when(projectService.getTableNames(userId, projectId)).thenReturn(tableNames);

        ProjectDTO expectedDto = new ProjectDTO();
        when(parserService.parseExcelFile(eq(projectOverview), eq(tableNames), any())).thenReturn(expectedDto);

        ProjectDTO result = orchestrator.uploadExcel(userId, projectId, file);

        assertSame(expectedDto, result);
        verify(projectService).getProjectOverviewDTO(userId, projectId);
        verify(projectService).getTableNames(userId, projectId);
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
        when(projectService.getTableNames(userId, projectId)).thenReturn(tableNames);

        when(parserService.parseExcelFile(any(), any(), any())).thenThrow(new IOException("bad file"));

        assertThrows(IOException.class, () -> orchestrator.uploadExcel(userId, projectId, file));
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
        when(projectService.getTableNames(userId, projectId)).thenReturn(tableNames);

        ProjectDTO projectData = new ProjectDTO();
        when(parserService.parseExcelFile(any(), any(), any())).thenReturn(projectData);
        doThrow(new ExcelValidationException("invalid", null)).when(dataValidatorService).validateData(projectData);

        assertThrows(ExcelValidationException.class, () -> orchestrator.uploadExcel(userId, projectId, file));
        verify(excelUploadService, never()).upload(anyLong(), any());
    }

    @Test
    void uploadExcel_throwsProjectNotFoundExceptionWhenProjectOverviewIsNull() throws Exception {
        String userId = "user-1";
        Long projectId = 1L;
        MultipartFile file = mock(MultipartFile.class);

        when(projectService.getProjectOverviewDTO(userId, projectId)).thenReturn(null);

        assertThrows(ProjectNotFoundException.class, () -> orchestrator.uploadExcel(userId, projectId, file));
        verify(projectService, never()).getTableNames(anyString(), anyLong());
        verify(parserService, never()).parseExcelFile(any(), any(), any());
        verify(dataValidatorService, never()).validateData(any());
        verify(excelUploadService, never()).upload(anyLong(), any());
    }
}
