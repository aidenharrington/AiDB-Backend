package com.aidb.aidb_backend.service.util;

import com.aidb.aidb_backend.exception.ExcelValidationException;
import com.aidb.aidb_backend.model.dto.ExcelDataDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExcelDataValidatorService {

    public void validateData(ExcelDataDto excelData) {
        for (ExcelDataDto.TableDto table : excelData.getTables()) {
            for (int i = 0; i < table.getRows().size(); i++) {
                List<Object> row = table.getRows().get(i);
                for (int j = 0; j < row.size(); j++) {
                    Object cellValue = row.get(j);
                    ExcelDataDto.ColumnTypeDto columnType = table.getColumns().get(j).getType();
                    if (!validateCell(cellValue, columnType)) {
                        String message = "Validation failed for row " + i + ", column " + j;
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
