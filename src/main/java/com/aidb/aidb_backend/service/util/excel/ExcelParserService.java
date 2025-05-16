package com.aidb.aidb_backend.service.util.excel;

import com.aidb.aidb_backend.model.dto.ExcelDataDto;
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
        String name = ExcelSanitizerService.formatString(sheet.getSheetName());
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

            String name = ExcelSanitizerService.formatColumnName(cell.getStringCellValue());
            column.setName(name);
            column.setType(inferColumnType(sheet, cell));
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

    private ExcelDataDto.ColumnTypeDto inferColumnType(Sheet sheet, Cell columnCell) {

        // Check the cell below the column to determine column type
        Cell firstDataCell = getCell(sheet, columnCell.getRowIndex() + 1, columnCell.getColumnIndex());

        if (firstDataCell == null) {
            return ExcelDataDto.ColumnTypeDto.TEXT;
        }


        return inferCellType(firstDataCell);
    }

    private Object getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        ExcelDataDto.ColumnTypeDto cellType = inferCellType(cell);

        return switch (cellType) {
            case TEXT -> ExcelSanitizerService.formatString(cell.getStringCellValue());
            case DATE -> ExcelSanitizerService.formatDate(cell.getDateCellValue());
            case NUMBER -> cell.getNumericCellValue();
        };
    }

    private Cell getCell(Sheet sheet, int rowIndex, int colIndex) {
        Row row = sheet.getRow(rowIndex);
        return row != null ? row.getCell(colIndex) : null;
    }

    private ExcelDataDto.ColumnTypeDto inferCellType(Cell cell) {
        if (cell == null) {
            return ExcelDataDto.ColumnTypeDto.TEXT;
        }

        if (Objects.requireNonNull(cell.getCellType()) == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return ExcelDataDto.ColumnTypeDto.DATE;
            } else {
                return ExcelDataDto.ColumnTypeDto.NUMBER;
            }
        }
        return ExcelDataDto.ColumnTypeDto.TEXT;
    }

}
