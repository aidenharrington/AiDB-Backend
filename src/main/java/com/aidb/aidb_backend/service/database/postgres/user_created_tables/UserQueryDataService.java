package com.aidb.aidb_backend.service.database.postgres.user_created_tables;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UserQueryDataService {
    private final JdbcTemplate queryUserJdbcTemplate;

    public UserQueryDataService(@Qualifier("queryUserJdbcTemplate") JdbcTemplate queryUserJdbcTemplate) {
        this.queryUserJdbcTemplate = queryUserJdbcTemplate;
    }

    public List<Map<String, Object>> executeSql(String safeSql) {
        return queryUserJdbcTemplate.queryForList(safeSql);
    }
}
