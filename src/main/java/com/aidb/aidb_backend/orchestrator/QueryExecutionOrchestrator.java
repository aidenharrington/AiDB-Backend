package com.aidb.aidb_backend.orchestrator;

import com.aidb.aidb_backend.exception.IllegalSqlException;
import com.aidb.aidb_backend.exception.ProjectNotFoundException;
import com.aidb.aidb_backend.exception.TableNotFoundException;
import com.aidb.aidb_backend.model.dto.QueryDTO;
import com.aidb.aidb_backend.model.firestore.Query;
import com.aidb.aidb_backend.model.firestore.Status;
import com.aidb.aidb_backend.service.database.firestore.QueryService;
import com.aidb.aidb_backend.service.database.postgres.TableMetadataService;
import com.aidb.aidb_backend.service.database.postgres.user_created_tables.UserQueryDataService;
import com.aidb.aidb_backend.service.util.sql.SqlTableNameReplacer;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class QueryExecutionOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(QueryExecutionOrchestrator.class);

    @Autowired
    QueryService queryService;

    @Autowired
    UserQueryDataService userQueryDataService;

    @Autowired
    TableMetadataService tableMetadataService;


    public List<Map<String, Object>> executeSafeSelectQuery(String userId, QueryDTO queryDTO) throws JSQLParserException {
        Query query = new Query(queryDTO);
        query.setUserId(userId);
        Map<String, String> tableNameMapping = tableMetadataService.getTableNameMapping(userId, Long.valueOf(query.getProjectId()));

        if (tableNameMapping == null || tableNameMapping.isEmpty()) {
            throw new TableNotFoundException();
        }


        String sql = formatSql(query.getSqlQuery(), tableNameMapping);

        List<Map<String, Object>> result = userQueryDataService.executeSql(sql);

        query.setStatus(Status.EXECUTED);
        saveQueryGracefully(query);

        return result;
    }

    private String formatSql(String rawSql, Map<String, String> tableNameMapping) throws JSQLParserException {
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

        SqlTableNameReplacer tableNameReplacer = new SqlTableNameReplacer(tableNameMapping);

        return tableNameReplacer.replaceTables(sql);
    }

    private String replaceTableNames(String sql, Map<String, String> tableNameMapping) {
        // Pattern to capture the FROM clause and everything until next keyword or end
        Pattern fromPattern = Pattern.compile("(?i)\\bFROM\\b\\s+(.+?)(?=\\bWHERE\\b|\\bGROUP\\b|\\bORDER\\b|$)");
        Matcher matcher = fromPattern.matcher(sql);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String fromContent = matcher.group(1).trim(); // e.g., "users u, contacts c"
            String[] tables = fromContent.split(","); // split by commas

            List<String> replacedTables = new ArrayList<>();
            for (String tablePart : tables) {
                tablePart = tablePart.trim();
                String[] parts = tablePart.split("\\s+"); // split by whitespace
                String displayName = parts[0];
                String aliasPart = tablePart.substring(displayName.length()); // preserve everything after table name

                String physicalName = tableNameMapping.getOrDefault(displayName, displayName);
                replacedTables.add(physicalName + aliasPart);
            }

            // Reconstruct the FROM clause
            matcher.appendReplacement(sb, "FROM " + String.join(", ", replacedTables));
        }
        matcher.appendTail(sb);
        return sb.toString();
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
