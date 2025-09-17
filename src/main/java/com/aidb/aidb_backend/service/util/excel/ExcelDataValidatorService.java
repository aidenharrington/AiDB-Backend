package com.aidb.aidb_backend.service.util.excel;

import com.aidb.aidb_backend.exception.ExcelValidationException;
import com.aidb.aidb_backend.model.dto.ProjectDTO;
import com.aidb.aidb_backend.model.dto.TableDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExcelDataValidatorService {

    public void validateData(ProjectDTO project) {
        for (TableDTO table : project.getTables()) {
            // Skip first row since that's the inferred type
            for (int rowIdx = 1; rowIdx < table.getRows().size(); rowIdx++) {
                List<Object> row = table.getRows().get(rowIdx);
                for (int colIdx = 0; colIdx < row.size(); colIdx++) {
                    Object cellValue = row.get(colIdx);
                    TableDTO.ColumnTypeDTO columnType = table.getColumns().get(colIdx).getType();
                    if (!validateCell(cellValue, columnType)) {
                        // Account for header row for error message
                        int actualRowIdx = rowIdx + 1;
                        String message = "Validation failed for row " + actualRowIdx + ", column " + colIdx +
                                ". Value: "+ cellValue + ". Expected type: "+ columnType;
                        throw new ExcelValidationException(message, HttpStatus.UNPROCESSABLE_ENTITY);
                    }
                }
            }
        }
    }

    private boolean validateCell(Object cellValue, TableDTO.ColumnTypeDTO columnType) {
        if (cellValue == null) {
            return false; // null is invalid unless you allow empty cells
        }

        return switch (columnType) {
            case TEXT -> cellValue instanceof String;
            case NUMBER -> cellValue instanceof Number;
            case DATE -> cellValue instanceof java.util.Date;
        };
    }
}
