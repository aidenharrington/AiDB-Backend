package com.aidb.aidb_backend.service.util.excel;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NameDeduplicationContext {

    private final Map<String, AtomicInteger> tableNameCounts = new HashMap<>();
    private final Map<String, Map<String, AtomicInteger>> sheetColumnNameCounts = new HashMap<>();
    private static final Pattern SUFFIX_PATTERN = Pattern.compile("^(.*?)(?:_(\\d+))?$");

    /**
     * Initialize deduplication context with existing table names (including suffixed ones).
     */
    public NameDeduplicationContext(Set<String> existingTableNames) {
        for (String name : existingTableNames) {
            String baseName = extractBaseName(name);
            int suffix = extractSuffix(name);
            tableNameCounts.compute(baseName, (k, v) -> {
                int currentMax = v == null ? 0 : v.get();
                return new AtomicInteger(Math.max(currentMax, suffix + 1));
            });
        }
    }

    /**
     * Deduplicate a name (table or column)
     */
    public String deduplicate(String sheetName, String baseName, boolean isTable) {
        if (isTable) {
            AtomicInteger counter = tableNameCounts.computeIfAbsent(baseName, k -> new AtomicInteger(0));
            int count = counter.getAndIncrement();
            return count == 0 ? baseName : baseName + "_" + count;
        } else {
            Map<String, AtomicInteger> columnCounts =
                    sheetColumnNameCounts.computeIfAbsent(sheetName, k -> new HashMap<>());

            AtomicInteger counter = columnCounts.computeIfAbsent(baseName, k -> new AtomicInteger(0));
            int count = counter.getAndIncrement();
            return count == 0 ? baseName : baseName + "_" + count;
        }
    }

    /**
     * Extract base name before a numeric suffix
     * e.g., "users_2" -> "users"
     */
    private String extractBaseName(String name) {
        Matcher matcher = SUFFIX_PATTERN.matcher(name);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return name;
    }

    /**
     * Extract numeric suffix if present
     * e.g., "users_2" -> 2
     */
    private int extractSuffix(String name) {
        Matcher matcher = SUFFIX_PATTERN.matcher(name);
        if (matcher.matches() && matcher.group(2) != null) {
            return Integer.parseInt(matcher.group(2));
        }
        return 0;
    }
}
