package com.aidb.aidb_backend.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class DynamicTableRepository {

    private final JdbcTemplate jdbcTemplate;

    public DynamicTableRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Fetches all rows from a dynamic table.
     * @param tableName table name in the database
     * @return list of maps, each representing a row
     */
    public List<Map<String, Object>> fetchAllRows(String tableName) {
        String sql = "SELECT * FROM \"" + tableName + "\"";
        try {
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            // Table might not exist
            return List.of();
        }
    }
}
