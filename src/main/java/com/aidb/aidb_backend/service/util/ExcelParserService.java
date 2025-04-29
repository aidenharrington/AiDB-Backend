package com.aidb.aidb_backend.service.util;

import com.aidb.aidb_backend.model.dto.ExcelDataDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
            ExcelDataDto.TableDto tableDto = new ExcelDataDto.TableDto();
            tableDto.setName(sheet.getSheetName());

            // Get columns and infer types based on first row
            List<ExcelDataDto.ColumnDto> columns = new ArrayList<>();
            Row headerRow = sheet.getRow(0);
            Iterator<Cell> headerIterator = headerRow.cellIterator();
            while (headerIterator.hasNext()) {
                Cell cell = headerIterator.next();
                ExcelDataDto.ColumnDto columnDto = new ExcelDataDto.ColumnDto();
                columnDto.setName(cell.getStringCellValue());
                columnDto.setType(inferColumnType(cell));
                columns.add(columnDto);
            }
            tableDto.setColumns(columns);

            // Get data for rows
            List<List<Object>> rows = new ArrayList<>();
            for (int i = 1; i <= sheet.getPhysicalNumberOfRows(); i++) {
                Row dataRow = sheet.getRow(i);
                List<Object> row = new ArrayList<>();
                for (int j = 0; j < columns.size(); j++) {
                    Cell dataCell = dataRow.getCell(j);
                    row.add(getCellValue(dataCell));
                }
                rows.add(row);
            }
            tableDto.setRows(rows);
            tables.add(tableDto);
        }

        excelData.setTables(tables);
        return excelData;
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
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
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
}
