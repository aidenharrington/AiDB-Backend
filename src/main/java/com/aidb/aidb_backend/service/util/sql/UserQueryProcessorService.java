package com.aidb.aidb_backend.service.util.sql;

import com.aidb.aidb_backend.exception.IllegalSqlException;
import com.aidb.aidb_backend.service.database.postgres.UserQueryDataService;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UserQueryProcessorService {

    @Autowired
    UserQueryDataService userQueryDataService;

    public List<Map<String, Object>> executeSafeSelectQuery(String rawSql) {
        String sql = rawSql.trim();

        if (containsUnsafeKeywords(sql)) {
            throw new IllegalSqlException("SQL contains disallowed operations.", HttpStatus.BAD_REQUEST);
        }

        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (!(statement instanceof Select)) {
                throw new IllegalSqlException("Only SELECT statements are allowed.", HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            throw new IllegalSqlException("Invalid SQL syntax: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        return userQueryDataService.executeSql(sql);
    }

    private boolean containsUnsafeKeywords(String sql) {
        String lower = sql.toLowerCase();
        return lower.contains("insert ") ||
                lower.contains("update ") ||
                lower.contains("delete ") ||
                lower.contains("drop ") ||
                lower.contains("truncate ") ||
                lower.contains("alter ") ||
                lower.contains("create ") ||
                lower.contains("grant ") ||
                lower.contains("revoke ") ||
                lower.contains("exec ") ||
                lower.contains(";") ||     // Disallow multiple statements
                lower.contains("--");      // Disallow comment injection
    }
}
