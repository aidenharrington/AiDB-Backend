package com.aidb.aidb_backend.service.util.excel;

import com.aidb.aidb_backend.model.dto.ExcelDataDTO;
import com.aidb.aidb_backend.model.dto.TableDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@Service
public class ExcelParserService {

    public ExcelDataDTO parseExcelFile(InputStream fileInputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(fileInputStream)) {
            ExcelDataDTO excelData = new ExcelDataDTO();
            List<TableDTO> tables = new ArrayList<>();

            // Iterate through each sheet (table)
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                TableDTO table = createTable(sheet);

                List<List<Object>> rows = parseRows(sheet, table.getColumns());
                table.setRows(rows);
                tables.add(table);

            }

            excelData.setTables(tables);

            return excelData;
        }
    }

    private TableDTO createTable(Sheet sheet) {
        TableDTO table = new TableDTO();
        String fileName = ExcelSanitizerService.formatString(sheet.getSheetName());
        table.setFileName(fileName);

        List<TableDTO.ColumnDto> columns = parseColumns(sheet);
        table.setColumns(columns);

        return table;
    }

    private List<TableDTO.ColumnDto> parseColumns (Sheet sheet) {
        List<TableDTO.ColumnDto> columns = new ArrayList<>();
        Row headerRow = sheet.getRow(0);
        Iterator<Cell> headerIterator = headerRow.cellIterator();

        while (headerIterator.hasNext()) {
            Cell cell = headerIterator.next();
            TableDTO.ColumnDto column = new TableDTO.ColumnDto();

            String name = ExcelSanitizerService.formatColumnName(cell.getStringCellValue());
            column.setName(name);
            column.setType(inferColumnType(sheet, cell));
            columns.add(column);
        }

        return columns;
    }

    private List<List<Object>> parseRows(Sheet sheet, List<TableDTO.ColumnDto> columns) {
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

    private TableDTO.ColumnTypeDto inferColumnType(Sheet sheet, Cell columnCell) {

        // Check the cell below the column to determine column type
        Cell firstDataCell = getCell(sheet, columnCell.getRowIndex() + 1, columnCell.getColumnIndex());

        if (firstDataCell == null) {
            return TableDTO.ColumnTypeDto.TEXT;
        }


        return inferCellType(firstDataCell);
    }

    private Object getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        TableDTO.ColumnTypeDto cellType = inferCellType(cell);

        if (cellType == TableDTO.ColumnTypeDto.TEXT) {
            return ExcelSanitizerService.formatString(cell.getStringCellValue());
        } else if (cellType == TableDTO.ColumnTypeDto.DATE) {
            return ExcelSanitizerService.formatDate(cell.getDateCellValue());
        } else if (cellType == TableDTO.ColumnTypeDto.NUMBER) {
            return cell.getNumericCellValue();
        }
        
        return null;
    }

    private Cell getCell(Sheet sheet, int rowIndex, int colIndex) {
        Row row = sheet.getRow(rowIndex);
        return row != null ? row.getCell(colIndex) : null;
    }

    private TableDTO.ColumnTypeDto inferCellType(Cell cell) {
        if (cell == null) {
            return TableDTO.ColumnTypeDto.TEXT;
        }

        if (Objects.requireNonNull(cell.getCellType()) == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return TableDTO.ColumnTypeDto.DATE;
            } else {
                return TableDTO.ColumnTypeDto.NUMBER;
            }
        }
        return TableDTO.ColumnTypeDto.TEXT;
    }

}
