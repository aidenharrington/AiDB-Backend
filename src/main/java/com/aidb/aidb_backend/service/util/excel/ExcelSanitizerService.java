package com.aidb.aidb_backend.service.util.excel;

import com.aidb.aidb_backend.exception.ExcelValidationException;
import com.aidb.aidb_backend.service.util.sql.SqlKeywords;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

@Service
public class ExcelSanitizerService {

    public static String formatColumnName(String value) {
        value = formatString(value);
        value = quoteColumnNameIfReserved(value);

        return value;
    }


    public static String formatString(String value) {
        if (value == null) {
            return null;
        }

        // Remove leading/trailing whitespaces
        String sanitizedValue = value.trim();

        // Replaces spaces with underscores
        sanitizedValue = sanitizedValue.replace(" ", "_");

        // Replace single quotes to avoid SQL injections
        sanitizedValue = sanitizedValue.replace("'", "''");

        // Throw an error if sanitized value is empty or contains invalid characters
        if (sanitizedValue.isEmpty() || !sanitizedValue.matches("^[a-zA-Z0-9_]*$")) {
            String message = "Illegal character in cell: " + sanitizedValue;
            throw new ExcelValidationException(message, HttpStatus.UNPROCESSABLE_ENTITY);
        }

        return sanitizedValue;
    }

    public static String formatDate(Date date) {
        if (date == null) {
            throw new ExcelValidationException("Invalid date value: null", HttpStatus.BAD_REQUEST);
        }

        // Format the date as a string in the format that PostgreSQL accepts
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(date); // Do not add quotes here
    }

    public static String quoteColumnNameIfReserved(String colName) {

        // List of common SQL reserved keywords
        Set<String> reservedKeywords = SqlKeywords.RESERVED_KEYWORDS;

        if (reservedKeywords.contains(colName.toUpperCase())) {
            return "\"" + colName + "\"";
        }

        return colName;
    }
}
