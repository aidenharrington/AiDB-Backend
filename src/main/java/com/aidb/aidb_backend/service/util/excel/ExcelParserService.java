package com.aidb.aidb_backend.service.util.excel;

import com.aidb.aidb_backend.model.dto.ProjectDTO;
import com.aidb.aidb_backend.model.dto.ProjectOverviewDTO;
import com.aidb.aidb_backend.model.dto.TableDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

    private List<TableDTO.ColumnDTO> parseColumns (Sheet sheet, NameDeduplicationContext deduplicationContext) {
        List<TableDTO.ColumnDTO> columns = new ArrayList<>();
        Row headerRow = sheet.getRow(0);

        if (headerRow != null) {
            for (Cell cell : headerRow) {
                TableDTO.ColumnDTO column = new TableDTO.ColumnDTO();

                String sanitizedName = ExcelNameService.sanitize(cell.getStringCellValue(), false);
                String dedupedName = deduplicationContext.deduplicate(sheet.getSheetName(), sanitizedName, false);
                column.setName(dedupedName);

                column.setType(inferColumnType(sheet, cell));
                columns.add(column);
            }
        }

        return columns;
    }

    private List<List<Object>> parseRows(Sheet sheet, List<TableDTO.ColumnDTO> columns) {
        List<List<Object>> rows = new ArrayList<>();

        for (int i = 1; i < sheet.getPhysicalNumberOfRows(); i++) {
            Row dataRow = sheet.getRow(i);

            if (dataRow != null) {
                List<Object> row = new ArrayList<>();
                for (int j = 0; j < columns.size(); j++) {
                    Cell dataCell = dataRow.getCell(j);
                    row.add(getCellValue(dataCell));
                }
                rows.add(row);
            }
        }

        return rows;
    }

    private TableDTO.ColumnTypeDTO inferColumnType(Sheet sheet, Cell columnCell) {

        // Check the cell below the column to determine column type
        Cell firstDataCell = getCell(sheet, columnCell.getRowIndex() + 1, columnCell.getColumnIndex());

        if (firstDataCell == null) {
            return TableDTO.ColumnTypeDTO.TEXT;
        }


        return inferCellType(firstDataCell);
    }

    private Object getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        TableDTO.ColumnTypeDTO cellType = inferCellType(cell);

        if (cellType == TableDTO.ColumnTypeDTO.TEXT) {
            return ExcelSanitizerService.formatString(cell.getStringCellValue());
        } else if (cellType == TableDTO.ColumnTypeDTO.DATE) {
            return ExcelSanitizerService.formatDate(cell.getDateCellValue());
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
