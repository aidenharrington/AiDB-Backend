package com.aidb.aidb_backend.service.util.excel;

import com.aidb.aidb_backend.model.dto.ExcelDataDto;
import com.aidb.aidb_backend.model.dto.TableDto;
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

    public ExcelDataDto parseExcelFile(InputStream fileInputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(fileInputStream)) {
            ExcelDataDto excelData = new ExcelDataDto();
            List<TableDto> tables = new ArrayList<>();

            // Iterate through each sheet (table)
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                TableDto table = createTable(sheet);

                List<List<Object>> rows = parseRows(sheet, table.getColumns());
                table.setRows(rows);
                tables.add(table);

            }

            excelData.setTables(tables);

            return excelData;
        }
    }

    private TableDto createTable(Sheet sheet) {
        TableDto table = new TableDto();
        String fileName = ExcelSanitizerService.formatString(sheet.getSheetName());
        table.setFileName(fileName);

        List<TableDto.ColumnDto> columns = parseColumns(sheet);
        table.setColumns(columns);

        return table;
    }

    private List<TableDto.ColumnDto> parseColumns (Sheet sheet) {
        List<TableDto.ColumnDto> columns = new ArrayList<>();
        Row headerRow = sheet.getRow(0);
        Iterator<Cell> headerIterator = headerRow.cellIterator();

        while (headerIterator.hasNext()) {
            Cell cell = headerIterator.next();
            TableDto.ColumnDto column = new TableDto.ColumnDto();

            String name = ExcelSanitizerService.formatColumnName(cell.getStringCellValue());
            column.setName(name);
            column.setType(inferColumnType(sheet, cell));
            columns.add(column);
        }

        return columns;
    }

    private List<List<Object>> parseRows(Sheet sheet, List<TableDto.ColumnDto> columns) {
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

    private TableDto.ColumnTypeDto inferColumnType(Sheet sheet, Cell columnCell) {

        // Check the cell below the column to determine column type
        Cell firstDataCell = getCell(sheet, columnCell.getRowIndex() + 1, columnCell.getColumnIndex());

        if (firstDataCell == null) {
            return TableDto.ColumnTypeDto.TEXT;
        }


        return inferCellType(firstDataCell);
    }

    private Object getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        TableDto.ColumnTypeDto cellType = inferCellType(cell);

        if (cellType == TableDto.ColumnTypeDto.TEXT) {
            return ExcelSanitizerService.formatString(cell.getStringCellValue());
        } else if (cellType == TableDto.ColumnTypeDto.DATE) {
            return ExcelSanitizerService.formatDate(cell.getDateCellValue());
        } else if (cellType == TableDto.ColumnTypeDto.NUMBER) {
            return cell.getNumericCellValue();
        }
        
        return null;
    }

    private Cell getCell(Sheet sheet, int rowIndex, int colIndex) {
        Row row = sheet.getRow(rowIndex);
        return row != null ? row.getCell(colIndex) : null;
    }

    private TableDto.ColumnTypeDto inferCellType(Cell cell) {
        if (cell == null) {
            return TableDto.ColumnTypeDto.TEXT;
        }

        if (Objects.requireNonNull(cell.getCellType()) == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return TableDto.ColumnTypeDto.DATE;
            } else {
                return TableDto.ColumnTypeDto.NUMBER;
            }
        }
        return TableDto.ColumnTypeDto.TEXT;
    }

}
