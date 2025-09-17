package com.aidb.aidb_backend.service.util.sql;

import com.aidb.aidb_backend.exception.TableNotFoundException;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.BinaryExpression;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Table name replacer for Select statements
 * Supports subqueries, joins, and nested queries.
 * CTEs not currently supported.
 */
public class SqlTableNameReplacer {

    private final Map<String, String> tableNameMapping;
    private final Map<String, String> aliasToTable = new HashMap<>();

    public SqlTableNameReplacer(Map<String, String> tableNameMapping) {
        this.tableNameMapping = tableNameMapping;
    }

    public String replaceTables(String sql) throws JSQLParserException {
        Statement stmt = CCJSqlParserUtil.parse(sql);

        if (!(stmt instanceof net.sf.jsqlparser.statement.select.PlainSelect
                || stmt instanceof SetOperationList
                || stmt instanceof ParenthesedSelect)) {
            throw new IllegalArgumentException("Only SELECT statements are supported.");
        }

        // Handle top-level statement
        handleSelectStatement(stmt);

        return stmt.toString();
    }

    private void handleSelectStatement(Object selectStmt) {
        if (selectStmt instanceof PlainSelect plain) {
            processPlainSelect(plain);
        } else if (selectStmt instanceof SetOperationList setOps) {
            for (var body : setOps.getSelects()) {
                handleSelectStatement(body);
            }
        } else if (selectStmt instanceof ParenthesedSelect ps) {
            handleSelectStatement(ps.getSelect());
        }
    }

    private void processPlainSelect(PlainSelect plain) {
        // First table
        if (plain.getFromItem() != null) {
            processFromItem(plain.getFromItem());
        }

        // All other tables and joins
        if (plain.getJoins() != null) {
            for (Join join : plain.getJoins()) {
                // Defensive: skip null right items
                FromItem right = join.getRightItem();
                if (right != null) {
                    processFromItem(right);
                }

                // Only process ON expression for real joins
                if (!join.isSimple() && join.getOnExpression() != null) {
                    processExpression(join.getOnExpression());
                }
            }
        }

        // WHERE
        if (plain.getWhere() != null) {
            processExpression(plain.getWhere());
        }

        // HAVING
        if (plain.getHaving() != null) {
            processExpression(plain.getHaving());
        }
    }

    private void processExpression(Expression expr) {
        if (expr instanceof Column column) {
            String tableName = column.getTable() != null ? column.getTable().getName() : null;
            if (tableName != null) {
                column.getTable().setName(mapTableNameIfFound(tableName));
            }
        } else if (expr instanceof BinaryExpression be) {
            processExpression(be.getLeftExpression());
            processExpression(be.getRightExpression());
        } else if (expr instanceof InExpression inExpr) {
            processExpression(inExpr.getLeftExpression());
            processExpression(inExpr.getRightExpression());
        }
        else if (expr instanceof ExistsExpression existsExpr) {
            processExpression(existsExpr.getRightExpression());
        } else if (expr instanceof ParenthesedSelect ps) {
            handleSelectStatement(ps.getSelect());
        }
    }

    private void processFromItem(FromItem fromItem) {
        if (fromItem instanceof Table table) {
            // Replace table name using mapping, preserving alias
            table.setName(mapTableNameIfFound(table.getName()));
        } else if (fromItem instanceof ParenthesedFromItem pfi) {
            // Only process the inner FromItem; joins are handled in enclosing PlainSelect
            processFromItem(pfi.getFromItem());
        } else if (fromItem instanceof ParenthesedSelect ps) {
            handleSelectStatement(ps.getSelectBody());
        }
    }

    private String mapTableNameIfFound(String tableName) throws TableNotFoundException {
        String mappedName = tableNameMapping.get(tableName);

        if (mappedName == null) {
            throw new TableNotFoundException(tableName);
        }

        return mappedName;
    }
}
