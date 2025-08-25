package com.aidb.aidb_backend.service.util.excel;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.regex.Pattern;

public class ExcelNameService {

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
