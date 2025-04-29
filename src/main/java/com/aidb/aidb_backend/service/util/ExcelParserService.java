package com.aidb.aidb_backend.service.util;

import com.aidb.aidb_backend.exception.ExcelValidationException;
import com.aidb.aidb_backend.model.dto.ExcelDataDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class ExcelParserService {

    public ExcelDataDto parseExcelFile(InputStream fileInputStream) throws IOException {
        Workbook workbook = new XSSFWorkbook(fileInputStream);
        ExcelDataDto excelData = new ExcelDataDto();
        List<ExcelDataDto.TableDto> tables = new ArrayList<>();

        // Iterate through each sheet (table)
        for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            ExcelDataDto.TableDto table = createTable(sheet);

            List<List<Object>> rows = parseRows(sheet, table.getColumns());
            table.setRows(rows);
            tables.add(table);

        }

        excelData.setTables(tables);

        return excelData;
    }

    private ExcelDataDto.TableDto createTable(Sheet sheet) {
        ExcelDataDto.TableDto table = new ExcelDataDto.TableDto();
        String name = formatData(sheet.getSheetName());
        table.setName(name);

        List<ExcelDataDto.ColumnDto> columns = parseColumns(sheet);
        table.setColumns(columns);

        return table;
    }

    private List<ExcelDataDto.ColumnDto> parseColumns (Sheet sheet) {
        List<ExcelDataDto.ColumnDto> columns = new ArrayList<>();
        Row headerRow = sheet.getRow(0);
        Iterator<Cell> headerIterator = headerRow.cellIterator();

        while (headerIterator.hasNext()) {
            Cell cell = headerIterator.next();
            ExcelDataDto.ColumnDto column = new ExcelDataDto.ColumnDto();

            String name = formatData(cell.getStringCellValue());
            column.setName(name);
            column.setType(inferColumnType(cell));
            columns.add(column);
        }

        return columns;
    }

    private List<List<Object>> parseRows(Sheet sheet, List<ExcelDataDto.ColumnDto> columns) {
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

    private ExcelDataDto.ColumnTypeDto inferColumnType(Cell cell) {
        if (cell.getCellType() == CellType.STRING) {
            return ExcelDataDto.ColumnTypeDto.TEXT;
        } else if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return ExcelDataDto.ColumnTypeDto.DATE;
            } else {
                return ExcelDataDto.ColumnTypeDto.NUMBER;
            }
        } else {
            return ExcelDataDto.ColumnTypeDto.TEXT;
        }
    }

    private Object getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {
            case STRING:
                return formatData(cell.getStringCellValue());
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                } else {
                    return cell.getNumericCellValue();
                }
            default:
                return null;
        }
    }

    private String formatData(String value) {
        if (value == null) {
            return null;
        }

        // Remove leading/trailing whitespaces
        String sanitizedValue = value.trim();

        // Replaces spaces with underscores
        sanitizedValue = sanitizedValue.replace(" ", "_");

        // Throw an error if sanitized value is empty or contains invalid characters
        if (sanitizedValue.isEmpty() || !sanitizedValue.matches("^[a-zA-Z0-9_]*$")) {
            String message = "Illegal character in cell: " + sanitizedValue;
            throw new ExcelValidationException(message, HttpStatus.UNPROCESSABLE_ENTITY);
        }

        return sanitizedValue;
    }

    private Object formatData(Object value) {
        if (value instanceof String) {
            return formatData((String) value);
        }
        return value;
    }

}
