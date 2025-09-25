package com.aidb.aidb_backend.service.util.excel;

import com.aidb.aidb_backend.exception.ExcelValidationException;
import com.aidb.aidb_backend.model.dto.ProjectDTO;
import com.aidb.aidb_backend.model.dto.ProjectOverviewDTO;
import com.aidb.aidb_backend.model.dto.TableDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ExcelParserServiceTest {

    private ExcelParserService excelParserService;
    private ProjectOverviewDTO projectOverview;
    private Set<String> tableNames;

    @BeforeEach
    void setUp() {
        excelParserService = new ExcelParserService();
        projectOverview = new ProjectOverviewDTO(1L, "Test Project", "user-1");
        tableNames = Set.of("existing_table");
    }

    @Test
    void parseExcelFile_withEmptyCells_handlesCorrectly() throws IOException {
        // Create Excel with empty cells in data rows
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("TestSheet");
            
            // Header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Name");
            headerRow.createCell(1).setCellValue("Age");
            headerRow.createCell(2).setCellValue("City");
            
            // Data rows with empty cells
            Row dataRow1 = sheet.createRow(1);
            dataRow1.createCell(0).setCellValue("John");
            // Cell 1 (Age) is empty
            dataRow1.createCell(2).setCellValue("New York");
            
            Row dataRow2 = sheet.createRow(2);
            dataRow2.createCell(0).setCellValue("Jane");
            dataRow2.createCell(1).setCellValue(25.0);
            // Cell 2 (City) is empty
            
            Row dataRow3 = sheet.createRow(3);
            // Cell 0 (Name) is empty
            dataRow3.createCell(1).setCellValue(30.0);
            dataRow3.createCell(2).setCellValue("Boston");

            InputStream inputStream = workbookToInputStream(workbook);
            
            ProjectDTO result = excelParserService.parseExcelFile(projectOverview, tableNames, inputStream);
            
            assertNotNull(result);
            assertEquals("user-1", result.getUserId());
            assertEquals(1, result.getTables().size());
            
            TableDTO table = result.getTables().get(0);
            assertEquals("testsheet", table.getFileName());
            assertEquals(3, table.getColumns().size());
            assertEquals(3, table.getRows().size());
            
            // Verify first row has null for empty Age cell
            assertEquals("John", table.getRows().get(0).get(0));
            assertNull(table.getRows().get(0).get(1));
            assertEquals("New_York", table.getRows().get(0).get(2));
            
            // Verify second row has null for empty City cell
            assertEquals("Jane", table.getRows().get(1).get(0));
            assertEquals(25.0, table.getRows().get(1).get(1));
            assertNull(table.getRows().get(1).get(2));
            
            // Verify third row has null for empty Name cell
            assertNull(table.getRows().get(2).get(0));
            assertEquals(30.0, table.getRows().get(2).get(1));
            assertEquals("Boston", table.getRows().get(2).get(2));
        }
    }

    @Test
    void parseExcelFile_withEmptyRows_skipsEmptyRows() throws IOException {
        // Create Excel with completely empty rows
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("TestSheet");
            
            // Header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Name");
            headerRow.createCell(1).setCellValue("Age");
            
            // Empty row (row 1) - should be skipped
            sheet.createRow(1); // Completely empty row
            
            // Data row with values
            Row dataRow = sheet.createRow(2);
            dataRow.createCell(0).setCellValue("John");
            dataRow.createCell(1).setCellValue(25.0);
            
            // Another empty row (row 3) - should be skipped
            sheet.createRow(3); // Completely empty row
            
            // Another data row
            Row dataRow2 = sheet.createRow(4);
            dataRow2.createCell(0).setCellValue("Jane");
            dataRow2.createCell(1).setCellValue(30.0);

            InputStream inputStream = workbookToInputStream(workbook);
            
            ProjectDTO result = excelParserService.parseExcelFile(projectOverview, tableNames, inputStream);
            
            assertNotNull(result);
            TableDTO table = result.getTables().get(0);
            assertEquals(2, table.getColumns().size());
            assertEquals(2, table.getRows().size()); // Only non-empty rows should be included
            
            // Verify only non-empty rows are included
            assertEquals("John", table.getRows().get(0).get(0));
            assertEquals(25.0, table.getRows().get(0).get(1));
            
            assertEquals("Jane", table.getRows().get(1).get(0));
            assertEquals(30.0, table.getRows().get(1).get(1));
        }
    }

    @Test
    void parseExcelFile_withMixedEmptyCellsAndRows_handlesCorrectly() throws IOException {
        // Create Excel with both empty cells and empty rows
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("TestSheet");
            
            // Header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Name");
            headerRow.createCell(1).setCellValue("Age");
            headerRow.createCell(2).setCellValue("City");
            
            // Data row with some empty cells
            Row dataRow1 = sheet.createRow(1);
            dataRow1.createCell(0).setCellValue("John");
            // Age is empty
            dataRow1.createCell(2).setCellValue("New York");
            
            // Completely empty row - should be skipped
            sheet.createRow(2);
            
            // Data row with all values
            Row dataRow2 = sheet.createRow(3);
            dataRow2.createCell(0).setCellValue("Jane");
            dataRow2.createCell(1).setCellValue(25.0);
            dataRow2.createCell(2).setCellValue("Boston");
            
            // Another empty row - should be skipped
            sheet.createRow(4);
            
            // Data row with some empty cells
            Row dataRow3 = sheet.createRow(5);
            // Name is empty
            dataRow3.createCell(1).setCellValue(30.0);
            dataRow3.createCell(2).setCellValue("Chicago");

            InputStream inputStream = workbookToInputStream(workbook);
            
            ProjectDTO result = excelParserService.parseExcelFile(projectOverview, tableNames, inputStream);
            
            assertNotNull(result);
            TableDTO table = result.getTables().get(0);
            assertEquals(3, table.getColumns().size());
            assertEquals(3, table.getRows().size()); // Only non-empty rows
            
            // Verify first row (John)
            assertEquals("John", table.getRows().get(0).get(0));
            assertNull(table.getRows().get(0).get(1));
            assertEquals("New_York", table.getRows().get(0).get(2));
            
            // Verify second row (Jane)
            assertEquals("Jane", table.getRows().get(1).get(0));
            assertEquals(25.0, table.getRows().get(1).get(1));
            assertEquals("Boston", table.getRows().get(1).get(2));
            
            // Verify third row (empty name)
            assertNull(table.getRows().get(2).get(0));
            assertEquals(30.0, table.getRows().get(2).get(1));
            assertEquals("Chicago", table.getRows().get(2).get(2));
        }
    }

    @Test
    void parseExcelFile_withNoHeaderRow_throwsException() throws IOException {
        // Create Excel without header row
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("TestSheet");
            
            // No header row, just data
            Row dataRow = sheet.createRow(0);
            dataRow.createCell(0).setCellValue("John");
            dataRow.createCell(1).setCellValue(25.0);

            InputStream inputStream = workbookToInputStream(workbook);
            
            ExcelValidationException exception = assertThrows(ExcelValidationException.class, 
                () -> excelParserService.parseExcelFile(projectOverview, tableNames, inputStream));
            
            assertTrue(exception.getMessage().contains("has no data"));
        }
    }

    @Test
    void parseExcelFile_withEmptyHeaderRow_throwsException() throws IOException {
        // Create Excel with empty header row
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("TestSheet");
            
            // Empty header row
            sheet.createRow(0); // Completely empty header row
            
            // Data row
            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue("John");
            dataRow.createCell(1).setCellValue(25.0);

            InputStream inputStream = workbookToInputStream(workbook);
            
            ExcelValidationException exception = assertThrows(ExcelValidationException.class, 
                () -> excelParserService.parseExcelFile(projectOverview, tableNames, inputStream));
            
            assertTrue(exception.getMessage().contains("no header row"));
        }
    }

    @Test
    void parseExcelFile_withColumnHavingNoData_throwsException() throws IOException {
        // Create Excel where one column has no data
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("TestSheet");
            
            // Header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Name");
            headerRow.createCell(1).setCellValue("Age");
            headerRow.createCell(2).setCellValue("EmptyColumn");
            
            // Data rows - EmptyColumn (index 2) has no data
            Row dataRow1 = sheet.createRow(1);
            dataRow1.createCell(0).setCellValue("John");
            dataRow1.createCell(1).setCellValue(25.0);
            // Cell 2 (EmptyColumn) is empty
            
            Row dataRow2 = sheet.createRow(2);
            dataRow2.createCell(0).setCellValue("Jane");
            dataRow2.createCell(1).setCellValue(30.0);
            // Cell 2 (EmptyColumn) is empty

            InputStream inputStream = workbookToInputStream(workbook);
            
            ExcelValidationException exception = assertThrows(ExcelValidationException.class, 
                () -> excelParserService.parseExcelFile(projectOverview, tableNames, inputStream));
            
            assertTrue(exception.getMessage().contains("has no data"));
            assertTrue(exception.getMessage().contains("EmptyColumn"));
        }
    }

    @Test
    void parseExcelFile_withColumnHavingOnlyEmptyCells_throwsException() throws IOException {
        // Create Excel where one column has only empty cells
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("TestSheet");
            
            // Header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Name");
            headerRow.createCell(1).setCellValue("Age");
            headerRow.createCell(2).setCellValue("EmptyColumn");
            
            // Data rows - EmptyColumn (index 2) has only empty cells
            Row dataRow1 = sheet.createRow(1);
            dataRow1.createCell(0).setCellValue("John");
            dataRow1.createCell(1).setCellValue(25.0);
            // Cell 2 (EmptyColumn) is empty - don't create cell
            
            Row dataRow2 = sheet.createRow(2);
            dataRow2.createCell(0).setCellValue("Jane");
            dataRow2.createCell(1).setCellValue(30.0);
            // Cell 2 (EmptyColumn) is empty - don't create cell

            InputStream inputStream = workbookToInputStream(workbook);
            
            ExcelValidationException exception = assertThrows(ExcelValidationException.class, 
                () -> excelParserService.parseExcelFile(projectOverview, tableNames, inputStream));
            
            assertTrue(exception.getMessage().contains("has no data"));
            assertTrue(exception.getMessage().contains("EmptyColumn"));
        }
    }

    @Test
    void parseExcelFile_withValidDataAndSomeEmptyCells_succeeds() throws IOException {
        // Create Excel with valid data where some cells are empty but each column has at least one valid cell
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("TestSheet");
            
            // Header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Name");
            headerRow.createCell(1).setCellValue("Age");
            headerRow.createCell(2).setCellValue("City");
            
            // Data rows - each column has at least one valid cell
            Row dataRow1 = sheet.createRow(1);
            dataRow1.createCell(0).setCellValue("John");
            dataRow1.createCell(1).setCellValue(25.0);
            // City is empty in this row
            
            Row dataRow2 = sheet.createRow(2);
            // Name is empty in this row
            dataRow2.createCell(1).setCellValue(30.0);
            dataRow2.createCell(2).setCellValue("Boston");
            
            Row dataRow3 = sheet.createRow(3);
            dataRow3.createCell(0).setCellValue("Jane");
            // Age is empty in this row
            dataRow3.createCell(2).setCellValue("New York");

            InputStream inputStream = workbookToInputStream(workbook);
            
            ProjectDTO result = excelParserService.parseExcelFile(projectOverview, tableNames, inputStream);
            
            assertNotNull(result);
            TableDTO table = result.getTables().get(0);
            assertEquals(3, table.getColumns().size());
            assertEquals(3, table.getRows().size());
            
            // Verify all rows are parsed correctly with nulls for empty cells
            assertEquals("John", table.getRows().get(0).get(0));
            assertEquals(25.0, table.getRows().get(0).get(1));
            assertNull(table.getRows().get(0).get(2));
            
            assertNull(table.getRows().get(1).get(0));
            assertEquals(30.0, table.getRows().get(1).get(1));
            assertEquals("Boston", table.getRows().get(1).get(2));
            
            assertEquals("Jane", table.getRows().get(2).get(0));
            assertNull(table.getRows().get(2).get(1));
            assertEquals("New_York", table.getRows().get(2).get(2));
        }
    }

    @Test
    void parseExcelFile_withOnlyEmptyRows_throwsException() throws IOException {
        // Create Excel with only empty rows after header
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("TestSheet");
            
            // Header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Name");
            headerRow.createCell(1).setCellValue("Age");
            
            // Only empty rows
            sheet.createRow(1); // Empty row
            sheet.createRow(2); // Empty row
            sheet.createRow(3); // Empty row

            InputStream inputStream = workbookToInputStream(workbook);
            
            ExcelValidationException exception = assertThrows(ExcelValidationException.class, 
                () -> excelParserService.parseExcelFile(projectOverview, tableNames, inputStream));
            
            assertTrue(exception.getMessage().contains("has no data"));
        }
    }

    @Test
    void parseExcelFile_withSingleValidRow_succeeds() throws IOException {
        // Create Excel with only one valid data row
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("TestSheet");
            
            // Header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Name");
            headerRow.createCell(1).setCellValue("Age");
            
            // Single data row
            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue("John");
            dataRow.createCell(1).setCellValue(25.0);

            InputStream inputStream = workbookToInputStream(workbook);
            
            ProjectDTO result = excelParserService.parseExcelFile(projectOverview, tableNames, inputStream);
            
            assertNotNull(result);
            TableDTO table = result.getTables().get(0);
            assertEquals(2, table.getColumns().size());
            assertEquals(1, table.getRows().size());
            
            assertEquals("John", table.getRows().get(0).get(0));
            assertEquals(25.0, table.getRows().get(0).get(1));
        }
    }

    private InputStream workbookToInputStream(Workbook workbook) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return new ByteArrayInputStream(outputStream.toByteArray());
    }
}