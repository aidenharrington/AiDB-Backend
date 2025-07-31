package com.aidb.aidb_backend.orchestrator;

import com.aidb.aidb_backend.exception.IllegalSqlException;
import com.aidb.aidb_backend.model.firestore.Query;
import com.aidb.aidb_backend.model.firestore.Status;
import com.aidb.aidb_backend.service.database.firestore.QueryService;
import com.aidb.aidb_backend.service.database.postgres.UserQueryDataService;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class QueryExecutionOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(QueryExecutionOrchestrator.class);

    private final QueryService queryService;
    private final UserQueryDataService userQueryDataService;

    public QueryExecutionOrchestrator(QueryService queryService, UserQueryDataService userQueryDataService) {
        this.queryService = queryService;
        this.userQueryDataService = userQueryDataService;
    }

    public List<Map<String, Object>> executeSafeSelectQuery(Query query) {
        String sql = query.getSqlQuery().trim();

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

        query.setStatus(Status.EXECUTED);

        saveQueryGracefully(query);

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

    private void saveQueryGracefully(Query query) {
        try {
            queryService.addOrUpdateQuery(query);
        } catch (Exception e) {
            logger.error("{}{}", "Failed saving query gracefully.", e.getMessage());
        }
    }
}
