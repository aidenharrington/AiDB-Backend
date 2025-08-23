package com.aidb.aidb_backend.orchestrator;

import com.aidb.aidb_backend.exception.ExcelValidationException;
import com.aidb.aidb_backend.model.dto.ExcelDataDTO;
import com.aidb.aidb_backend.model.dto.ProjectDTO;
import com.aidb.aidb_backend.model.postgres.Project;
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
    void uploadExcel_Deprecated_success_callsServicesInOrder_andReturnsDto() throws Exception {
        String userId = "user-1";
        Long projectId = 42L;
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        ExcelDataDTO excelData = new ExcelDataDTO();
        when(parserService.parseExcelFile(any())).thenReturn(excelData);

        Project project = new Project();
        when(projectService.getProject(userId, projectId)).thenReturn(project);

        ProjectDTO expectedDto = new ProjectDTO();
        when(projectService.convertProjectToDTO(project)).thenReturn(expectedDto);

        ProjectDTO result = orchestrator.uploadExcel_Deprecated(userId, projectId, file);

        assertSame(expectedDto, result);
        verify(parserService).parseExcelFile(any());
        verify(dataValidatorService).validateData(excelData);
        verify(projectService).getProject(userId, projectId);
        verify(excelUploadService).upload(project, excelData);
        verify(projectService).convertProjectToDTO(project);
    }

    @Test
    void uploadExcel_Deprecated_propagatesIOExceptionFromParser() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(parserService.parseExcelFile(any())).thenThrow(new IOException("bad file"));

        assertThrows(IOException.class, () -> orchestrator.uploadExcel_Deprecated("user", 1L, file));
        verifyNoInteractions(dataValidatorService, projectService, excelUploadService);
    }

    @Test
    void uploadExcel_Deprecated_propagatesValidationException() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        ExcelDataDTO excelData = new ExcelDataDTO();
        when(parserService.parseExcelFile(any())).thenReturn(excelData);
        doThrow(new ExcelValidationException("invalid", null)).when(dataValidatorService).validateData(excelData);

        assertThrows(ExcelValidationException.class, () -> orchestrator.uploadExcel_Deprecated("user", 1L, file));
        verify(projectService, never()).getProject(anyString(), anyLong());
        verify(excelUploadService, never()).upload(any(), any());
    }
}
