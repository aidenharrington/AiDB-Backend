package com.aidb.aidb_backend.service.util.excel;

import java.util.List;
import java.util.regex.Pattern;

public class ExcelNameService {

    // TODO - replace generateUniqueDisplayName
    public String generateTableDisplayName(String sheetName, List<String> displayNames) {
        return null;
    }

    // TODO - replace generateTableName
    public String generatePhysicalTableName(Long projectId, String displayName) {
        return null;
    }

    private static final Pattern INVALID_CHARS_PATTERN = Pattern.compile("[^a-z0-9_]+");

    public static String sanitize(String value, boolean isTable) {
        if (value == null || value.isEmpty()) {
            value = "default";
        }

        String name = value.toLowerCase();
        name = INVALID_CHARS_PATTERN.matcher(name).replaceAll("_");
        name = name.replaceAll("^_+|_+$", ""); // trim leading/trailing _

        if (!name.isEmpty() && Character.isDigit(name.charAt(0))) {
            name = (isTable ? "t_" : "c_") + name;
        }

        return name;
    }
}
