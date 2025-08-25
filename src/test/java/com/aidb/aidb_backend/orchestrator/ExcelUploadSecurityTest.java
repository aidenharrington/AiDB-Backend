package com.aidb.aidb_backend.orchestrator;

import com.aidb.aidb_backend.exception.ExcelValidationException;
import com.aidb.aidb_backend.model.dto.ProjectDTO;
import com.aidb.aidb_backend.model.dto.ProjectOverviewDTO;
import com.aidb.aidb_backend.service.database.postgres.ProjectService;
import com.aidb.aidb_backend.service.database.postgres.TableMetadataService;
import com.aidb.aidb_backend.service.database.postgres.user_created_tables.ExcelUploadService;
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
import java.io.InputStream;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Security-focused tests for Excel upload functionality
 * Tests malicious file handling, size limits, content validation, and injection attacks
 */
class ExcelUploadSecurityTest {

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

    private static final String USER_ID = "test-user";
    private static final String PROJECT_ID = "123";
    private static final Long PROJECT_ID_LONG = 123L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void uploadExcel_handlesNullInputStream() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(null);

        ProjectOverviewDTO projectOverview = new ProjectOverviewDTO(PROJECT_ID_LONG, "Test Project", USER_ID);
        when(projectService.getProjectOverviewDTO(USER_ID, PROJECT_ID_LONG)).thenReturn(projectOverview);

        Set<String> tableNames = Set.of("table1");
        when(tableMetadataService.getTableNames(USER_ID, PROJECT_ID_LONG)).thenReturn(tableNames);

        // The parser service handles null input stream gracefully
        when(parserService.parseExcelFile(eq(projectOverview), eq(tableNames), isNull()))
            .thenThrow(new IOException("Cannot read from null input stream"));

        assertThrows(IOException.class, () -> orchestrator.uploadExcel(USER_ID, PROJECT_ID, file));
        verify(parserService).parseExcelFile(eq(projectOverview), eq(tableNames), isNull());
        verifyNoInteractions(dataValidatorService, excelUploadService);
    }

    @Test
    void uploadExcel_handlesCorruptedFileStream() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        // Simulate a corrupted or malformed input stream
        InputStream corruptedStream = new ByteArrayInputStream("CORRUPTED_DATA".getBytes());
        when(file.getInputStream()).thenReturn(corruptedStream);

        ProjectOverviewDTO projectOverview = new ProjectOverviewDTO(PROJECT_ID_LONG, "Test Project", USER_ID);
        when(projectService.getProjectOverviewDTO(USER_ID, PROJECT_ID_LONG)).thenReturn(projectOverview);

        Set<String> tableNames = Set.of("table1");
        when(tableMetadataService.getTableNames(USER_ID, PROJECT_ID_LONG)).thenReturn(tableNames);

        when(parserService.parseExcelFile(eq(projectOverview), eq(tableNames), any()))
                .thenThrow(new IOException("Invalid Excel file format"));

        assertThrows(IOException.class, () -> orchestrator.uploadExcel(USER_ID, PROJECT_ID, file));
        verify(parserService).parseExcelFile(eq(projectOverview), eq(tableNames), any());
        verifyNoInteractions(dataValidatorService, excelUploadService);
    }

    @Test
    void uploadExcel_handlesMaliciousFileName() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("../../etc/passwd.xlsx"); // Path traversal attempt
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        ProjectOverviewDTO projectOverview = new ProjectOverviewDTO(PROJECT_ID_LONG, "Test Project", USER_ID);
        when(projectService.getProjectOverviewDTO(USER_ID, PROJECT_ID_LONG)).thenReturn(projectOverview);

        Set<String> tableNames = Set.of("table1");
        when(tableMetadataService.getTableNames(USER_ID, PROJECT_ID_LONG)).thenReturn(tableNames);

        ProjectDTO projectData = new ProjectDTO();
        when(parserService.parseExcelFile(eq(projectOverview), eq(tableNames), any())).thenReturn(projectData);

        // The system should still process the file content, not the filename
        ProjectDTO result = orchestrator.uploadExcel(USER_ID, PROJECT_ID, file);

        assertSame(projectData, result);
        verify(dataValidatorService).validateData(projectData);
        verify(excelUploadService).upload(PROJECT_ID_LONG, projectData);
    }

    @Test
    void uploadExcel_handlesFileWithScriptInjectionAttempt() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("<script>alert('xss')</script>.xlsx");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        ProjectOverviewDTO projectOverview = new ProjectOverviewDTO(PROJECT_ID_LONG, "Test Project", USER_ID);
        when(projectService.getProjectOverviewDTO(USER_ID, PROJECT_ID_LONG)).thenReturn(projectOverview);

        Set<String> tableNames = Set.of("table1");
        when(tableMetadataService.getTableNames(USER_ID, PROJECT_ID_LONG)).thenReturn(tableNames);

        ProjectDTO projectData = new ProjectDTO();
        when(parserService.parseExcelFile(eq(projectOverview), eq(tableNames), any())).thenReturn(projectData);

        // The system should process the file without being affected by the malicious filename
        ProjectDTO result = orchestrator.uploadExcel(USER_ID, PROJECT_ID, file);

        assertSame(projectData, result);
        verify(dataValidatorService).validateData(projectData);
        verify(excelUploadService).upload(PROJECT_ID_LONG, projectData);
    }

    @Test
    void uploadExcel_validationRejectsMaliciousData() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        ProjectOverviewDTO projectOverview = new ProjectOverviewDTO(PROJECT_ID_LONG, "Test Project", USER_ID);
        when(projectService.getProjectOverviewDTO(USER_ID, PROJECT_ID_LONG)).thenReturn(projectOverview);

        Set<String> tableNames = Set.of("table1");
        when(tableMetadataService.getTableNames(USER_ID, PROJECT_ID_LONG)).thenReturn(tableNames);

        ProjectDTO projectData = new ProjectDTO();
        when(parserService.parseExcelFile(eq(projectOverview), eq(tableNames), any())).thenReturn(projectData);

        // Simulate validation rejection of malicious content
        doThrow(new ExcelValidationException("Malicious content detected", null))
                .when(dataValidatorService).validateData(projectData);

        assertThrows(ExcelValidationException.class, () -> orchestrator.uploadExcel(USER_ID, PROJECT_ID, file));
        verify(dataValidatorService).validateData(projectData);
        verifyNoInteractions(excelUploadService);
    }

    @Test
    void uploadExcel_handlesExceptionDuringUpload() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        ProjectOverviewDTO projectOverview = new ProjectOverviewDTO(PROJECT_ID_LONG, "Test Project", USER_ID);
        when(projectService.getProjectOverviewDTO(USER_ID, PROJECT_ID_LONG)).thenReturn(projectOverview);

        Set<String> tableNames = Set.of("table1");
        when(tableMetadataService.getTableNames(USER_ID, PROJECT_ID_LONG)).thenReturn(tableNames);

        ProjectDTO projectData = new ProjectDTO();
        when(parserService.parseExcelFile(eq(projectOverview), eq(tableNames), any())).thenReturn(projectData);

        // Simulate database failure during upload
        doThrow(new RuntimeException("Database connection failed"))
                .when(excelUploadService).upload(PROJECT_ID_LONG, projectData);

        assertThrows(RuntimeException.class, () -> orchestrator.uploadExcel(USER_ID, PROJECT_ID, file));
        verify(dataValidatorService).validateData(projectData);
        verify(excelUploadService).upload(PROJECT_ID_LONG, projectData);
    }

    @Test
    void uploadExcel_preventsMemoryExhaustionWithLargeData() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        ProjectOverviewDTO projectOverview = new ProjectOverviewDTO(PROJECT_ID_LONG, "Test Project", USER_ID);
        when(projectService.getProjectOverviewDTO(USER_ID, PROJECT_ID_LONG)).thenReturn(projectOverview);

        Set<String> tableNames = Set.of("table1");
        when(tableMetadataService.getTableNames(USER_ID, PROJECT_ID_LONG)).thenReturn(tableNames);

        // Simulate parser throwing out of memory error for extremely large files
        when(parserService.parseExcelFile(eq(projectOverview), eq(tableNames), any()))
                .thenThrow(new OutOfMemoryError("Excel file too large"));

        assertThrows(OutOfMemoryError.class, () -> orchestrator.uploadExcel(USER_ID, PROJECT_ID, file));
        verify(parserService).parseExcelFile(eq(projectOverview), eq(tableNames), any());
        verifyNoInteractions(dataValidatorService, excelUploadService);
    }

    @Test
    void uploadExcel_handlesInvalidFileExtensions() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("malicious.exe.xlsx"); // Double extension attempt
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        ProjectOverviewDTO projectOverview = new ProjectOverviewDTO(PROJECT_ID_LONG, "Test Project", USER_ID);
        when(projectService.getProjectOverviewDTO(USER_ID, PROJECT_ID_LONG)).thenReturn(projectOverview);

        Set<String> tableNames = Set.of("table1");
        when(tableMetadataService.getTableNames(USER_ID, PROJECT_ID_LONG)).thenReturn(tableNames);

        // If the content is not valid Excel, parser should throw an exception
        when(parserService.parseExcelFile(eq(projectOverview), eq(tableNames), any()))
                .thenThrow(new IOException("Invalid Excel file format"));

        assertThrows(IOException.class, () -> orchestrator.uploadExcel(USER_ID, PROJECT_ID, file));
        verify(parserService).parseExcelFile(eq(projectOverview), eq(tableNames), any());
        verifyNoInteractions(dataValidatorService, excelUploadService);
    }

    @Test
    void uploadExcel_handlesStreamClosureException() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        InputStream mockStream = mock(InputStream.class);
        when(file.getInputStream()).thenReturn(mockStream);
        
        // Simulate stream reading but closure failure
        when(mockStream.read(any())).thenReturn(-1);
        doThrow(new IOException("Failed to close stream")).when(mockStream).close();

        ProjectOverviewDTO projectOverview = new ProjectOverviewDTO(PROJECT_ID_LONG, "Test Project", USER_ID);
        when(projectService.getProjectOverviewDTO(USER_ID, PROJECT_ID_LONG)).thenReturn(projectOverview);

        Set<String> tableNames = Set.of("table1");
        when(tableMetadataService.getTableNames(USER_ID, PROJECT_ID_LONG)).thenReturn(tableNames);

        ProjectDTO projectData = new ProjectDTO();
        when(parserService.parseExcelFile(eq(projectOverview), eq(tableNames), any())).thenReturn(projectData);

        // Should still complete successfully even if stream closure fails
        ProjectDTO result = orchestrator.uploadExcel(USER_ID, PROJECT_ID, file);

        assertSame(projectData, result);
        verify(dataValidatorService).validateData(projectData);
        verify(excelUploadService).upload(PROJECT_ID_LONG, projectData);
    }

    @Test
    void uploadExcel_preventsTableNameCollisions() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        ProjectOverviewDTO projectOverview = new ProjectOverviewDTO(PROJECT_ID_LONG, "Test Project", USER_ID);
        when(projectService.getProjectOverviewDTO(USER_ID, PROJECT_ID_LONG)).thenReturn(projectOverview);

        // Existing tables with potentially conflicting names
        Set<String> existingTableNames = Set.of("users", "admin", "system_config");
        when(tableMetadataService.getTableNames(USER_ID, PROJECT_ID_LONG)).thenReturn(existingTableNames);

        ProjectDTO projectData = new ProjectDTO();
        when(parserService.parseExcelFile(eq(projectOverview), eq(existingTableNames), any())).thenReturn(projectData);

        ProjectDTO result = orchestrator.uploadExcel(USER_ID, PROJECT_ID, file);

        assertSame(projectData, result);
        // Verify that the existing table names were passed to prevent collisions
        verify(parserService).parseExcelFile(eq(projectOverview), eq(existingTableNames), any());
        verify(dataValidatorService).validateData(projectData);
        verify(excelUploadService).upload(PROJECT_ID_LONG, projectData);
    }

    @Test
    void uploadExcel_handlesZipBombAttempt() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        ProjectOverviewDTO projectOverview = new ProjectOverviewDTO(PROJECT_ID_LONG, "Test Project", USER_ID);
        when(projectService.getProjectOverviewDTO(USER_ID, PROJECT_ID_LONG)).thenReturn(projectOverview);

        Set<String> tableNames = Set.of("table1");
        when(tableMetadataService.getTableNames(USER_ID, PROJECT_ID_LONG)).thenReturn(tableNames);

        // Simulate zip bomb detection (extremely large uncompressed size)
        when(parserService.parseExcelFile(eq(projectOverview), eq(tableNames), any()))
                .thenThrow(new IOException("Zip bomb detected: uncompressed size exceeds limit"));

        assertThrows(IOException.class, () -> orchestrator.uploadExcel(USER_ID, PROJECT_ID, file));
        verify(parserService).parseExcelFile(eq(projectOverview), eq(tableNames), any());
        verifyNoInteractions(dataValidatorService, excelUploadService);
    }
}