package com.aidb.aidb_backend.service.util.excel;

import com.aidb.aidb_backend.exception.ExcelValidationException;
import com.aidb.aidb_backend.model.dto.ExcelDataDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExcelDataValidatorService {

    public void validateData(ExcelDataDto excelData) {
        for (ExcelDataDto.TableDto table : excelData.getTables()) {
            for (int rowIdx = 1; rowIdx < table.getRows().size(); rowIdx++) {
                List<Object> row = table.getRows().get(rowIdx);
                for (int colIdx = 0; colIdx < row.size(); colIdx++) {
                    Object cellValue = row.get(colIdx);
                    ExcelDataDto.ColumnTypeDto columnType = table.getColumns().get(colIdx).getType();
                    if (!validateCell(cellValue, columnType)) {
                        String message = "Validation failed for row " + rowIdx + ", column " + colIdx;
                        throw new ExcelValidationException(message, HttpStatus.UNPROCESSABLE_ENTITY);
                    }
                }
            }
        }
    }

    private boolean validateCell(Object cellValue, ExcelDataDto.ColumnTypeDto columnType) {
        return switch (cellValue) {
            case String s when columnType == ExcelDataDto.ColumnTypeDto.TEXT -> true;
            case Double v when columnType == ExcelDataDto.ColumnTypeDto.NUMBER -> true;
            case java.util.Date date when columnType == ExcelDataDto.ColumnTypeDto.DATE -> true;
            case null, default -> false;
        };
    }
}
