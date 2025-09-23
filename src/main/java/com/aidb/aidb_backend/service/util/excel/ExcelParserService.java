package com.aidb.aidb_backend.service.util.excel;

import com.aidb.aidb_backend.exception.ExcelValidationException;
import com.aidb.aidb_backend.model.dto.ProjectDTO;
import com.aidb.aidb_backend.model.dto.ProjectOverviewDTO;
import com.aidb.aidb_backend.model.dto.TableDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.IOException;
import java.util.*;

@Service
public class ExcelParserService {

    public ProjectDTO parseExcelFile(ProjectOverviewDTO projectOverview, Set<String> tableNames, InputStream fileInputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(fileInputStream)) {
            NameDeduplicationContext deduplicationContext = new NameDeduplicationContext(tableNames);

            ProjectDTO project = new ProjectDTO();
            project.setUserId(projectOverview.getUserId());
            List<TableDTO> tables = new ArrayList<>();

            // Iterate through each sheet (table)
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                TableDTO table = createTable(sheet, deduplicationContext);

                tables.add(table);

            }

            project.setTables(tables);

            return project;
        }
    }

    private TableDTO createTable(Sheet sheet, NameDeduplicationContext deduplicationContext) {
        TableDTO table = new TableDTO();
        String sanitizedName = ExcelNameService.sanitize(sheet.getSheetName(), true);
        table.setFileName(sanitizedName);
        String dedupedName = deduplicationContext.deduplicate(sheet.getSheetName(), sanitizedName, true);
        table.setDisplayName(dedupedName);

        List<TableDTO.ColumnDTO> columns = parseColumns(sheet, deduplicationContext);
        table.setColumns(columns);

        List<List<Object>> rows = parseRows(sheet, table.getColumns());
        table.setRows(rows);

        return table;
    }

    private List<TableDTO.ColumnDTO> parseColumns(Sheet sheet, NameDeduplicationContext deduplicationContext) {
        List<TableDTO.ColumnDTO> columns = new ArrayList<>();

        // Get the first row (expected header)
        Row headerRow = sheet.getRow(0);

        // Validate header row exists
        if (headerRow == null) {
            throw new ExcelValidationException("Excel sheet '" + sheet.getSheetName() + "' has no header row (row 1).");
        }

        boolean hasValidHeader = false;

        for (Cell cell : headerRow) {
            String rawName = cell.getStringCellValue().trim();
            if (!rawName.isEmpty()) {
                hasValidHeader = true;

                TableDTO.ColumnDTO column = new TableDTO.ColumnDTO();
                String sanitizedName = ExcelNameService.sanitize(rawName, false);
                String dedupedName = deduplicationContext.deduplicate(sheet.getSheetName(), sanitizedName, false);

                column.setName(dedupedName);
                column.setType(inferColumnType(sheet, cell));
                columns.add(column);
            }
        }

        if (!hasValidHeader) {
            throw new ExcelValidationException("Excel sheet '" + sheet.getSheetName() + "' has no header row (row 1).");        }

        return columns;
    }


    private List<List<Object>> parseRows(Sheet sheet, List<TableDTO.ColumnDTO> columns) {
        int columnCount = columns.size();
        List<List<Object>> rows = new ArrayList<>();

        for (Row dataRow : sheet) { // iterates only non-null rows
            if (dataRow.getRowNum() == 0) continue; // skip header row

            List<Object> row = new ArrayList<>(columnCount);
            boolean hasValue = false;

            for (int j = 0; j < columnCount; j++) {
                Cell dataCell = dataRow.getCell(j, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                Object value = getCellValue(dataCell);
                row.add(value);

                if (!hasValue && value != null && !value.toString().isEmpty()) {
                    hasValue = true;
                }
            }

            if (hasValue) {
                rows.add(row);
            }
        }

        return rows;
    }

    private TableDTO.ColumnTypeDTO inferColumnType(Sheet sheet, Cell headerCell) {
        int columnIndex = headerCell.getColumnIndex();
        int firstRowIndex = headerCell.getRowIndex() + 1;
        int lastRowIndex = sheet.getLastRowNum();

        for (int rowIndex = firstRowIndex; rowIndex <= lastRowIndex; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;

            Cell dataCell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

            // First non-empty cell determines type
            if (dataCell != null && getCellValue(dataCell) != null && !getCellValue(dataCell).toString().isEmpty()) {
                return inferCellType(dataCell);
            }
        }

        // If we reach here, all cells below header are empty → invalid Excel
        throw new ExcelValidationException("Column '" + headerCell.getStringCellValue() + "' in sheet '" +
                sheet.getSheetName() + "' has no data.");
    }

    private Object getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        TableDTO.ColumnTypeDTO cellType = inferCellType(cell);

        if (cellType == TableDTO.ColumnTypeDTO.TEXT) {
            return ExcelSanitizerService.formatString(cell.getStringCellValue());
        } else if (cellType == TableDTO.ColumnTypeDTO.DATE) {
            return cell.getDateCellValue();
        } else if (cellType == TableDTO.ColumnTypeDTO.NUMBER) {
            return cell.getNumericCellValue();
        }
        
        return null;
    }

    private Cell getCell(Sheet sheet, int rowIndex, int colIndex) {
        Row row = sheet.getRow(rowIndex);
        return row != null ? row.getCell(colIndex) : null;
    }

    private TableDTO.ColumnTypeDTO inferCellType(Cell cell) {
        if (cell == null) {
            return TableDTO.ColumnTypeDTO.TEXT;
        }

        if (Objects.requireNonNull(cell.getCellType()) == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return TableDTO.ColumnTypeDTO.DATE;
            } else {
                return TableDTO.ColumnTypeDTO.NUMBER;
            }
        }
        return TableDTO.ColumnTypeDTO.TEXT;
    }

}
