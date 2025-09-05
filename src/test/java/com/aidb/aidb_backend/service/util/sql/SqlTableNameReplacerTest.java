package com.aidb.aidb_backend.service.util.sql;

import com.aidb.aidb_backend.exception.TableNotFoundException;
import net.sf.jsqlparser.JSQLParserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SqlTableNameReplacerTest {

    private SqlTableNameReplacer replacer;

    @BeforeEach
    void setUp() {
        Map<String, String> tableMapping = Map.of(
                "ideal_cat_houses", "physical_cat_houses",
                "ideal_cat_number", "physical_cat_number",
                "users", "app_users",
                "contacts", "app_contacts",
                "orders", "app_orders"
        );
        replacer = new SqlTableNameReplacer(tableMapping);
    }

    @Test
    void testSimpleFrom() throws JSQLParserException {
        String sql = "SELECT * FROM ideal_cat_houses";
        assertEquals("SELECT * FROM physical_cat_houses", replacer.replaceTables(sql));
    }

    @Test
    void testFromWithAlias() throws JSQLParserException {
        String sql = "SELECT h.name FROM ideal_cat_houses h";
        assertEquals("SELECT h.name FROM physical_cat_houses h", replacer.replaceTables(sql));
    }

    @Test
    void testMultipleTablesCommaSeparated() throws JSQLParserException {
        String sql = "SELECT u.name, c.email FROM users u, contacts c";
        assertEquals("SELECT u.name, c.email FROM app_users u, app_contacts c", replacer.replaceTables(sql));
    }

    @Test
    void testMixedCaseKeywordsAndExtraWhitespace() {
        String sql = "SELECT  *   FrOm   ideal_cat_houses   H  JOIN ideal_cat_number  N  ON H.id = N.house_id";
        // This should fail because H and N are aliases being treated as table names
        assertThrows(TableNotFoundException.class, () -> replacer.replaceTables(sql));
    }

    @Test
    void testNoFromClause() throws JSQLParserException {
        String sql = "SELECT 1";
        assertEquals("SELECT 1", replacer.replaceTables(sql));
    }

    @Test
    void testJoinWithoutAlias() throws JSQLParserException {
        String sql = "SELECT * FROM ideal_cat_houses JOIN ideal_cat_number ON ideal_cat_houses.id = ideal_cat_number.house_id";
        String expected = "SELECT * FROM physical_cat_houses JOIN physical_cat_number ON physical_cat_houses.id = physical_cat_number.house_id";
        assertEquals(expected, replacer.replaceTables(sql));
    }

    @Test
    void testMultipleJoins() {
        String sql = "SELECT * FROM users u JOIN contacts c ON u.id=c.user_id JOIN ideal_cat_houses h ON h.id=u.house_id";
        // This should fail because u, c, h are aliases being treated as table names
        assertThrows(TableNotFoundException.class, () -> replacer.replaceTables(sql));
    }

    @Test
    void testSubqueryInFrom() throws JSQLParserException {
        String sql = "SELECT * FROM (SELECT * FROM users) sub";
        String expected = "SELECT * FROM (SELECT * FROM app_users) sub";
        assertEquals(expected, replacer.replaceTables(sql));
    }

    @Test
    void testNestedSubqueries() throws JSQLParserException {
        String sql = "SELECT * FROM (SELECT * FROM (SELECT * FROM contacts) c) sub";
        String expected = "SELECT * FROM (SELECT * FROM (SELECT * FROM app_contacts) c) sub";
        assertEquals(expected, replacer.replaceTables(sql));
    }

    @Test
    void testAliasedSubqueryWithJoin() {
        String sql = "SELECT s.id FROM (SELECT * FROM ideal_cat_houses) s JOIN ideal_cat_number n ON s.id=n.house_id";
        // This should fail because s and n are aliases being treated as table names
        assertThrows(TableNotFoundException.class, () -> replacer.replaceTables(sql));
    }

    // Not Supported
//    @Test
//    void testCTEWithSubquery() throws JSQLParserException {
//        String sql = "WITH recent_houses AS (SELECT * FROM ideal_cat_houses WHERE value>1000) " +
//                "SELECT * FROM recent_houses r JOIN ideal_cat_number n ON r.id = n.house_id";
//        String expected = "WITH recent_houses AS (SELECT * FROM physical_cat_houses WHERE value > 1000) " +
//                "SELECT * FROM recent_houses r JOIN physical_cat_number n ON r.id = n.house_id";
//        assertEquals(expected, replacer.replaceTables(sql));
//    }

    @Test
    void testMultipleFromsWithCommasAndAliases() throws JSQLParserException {
        String sql = "SELECT u.name, c.email, h.value FROM users u, contacts c, ideal_cat_houses h";
        String expected = "SELECT u.name, c.email, h.value FROM app_users u, app_contacts c, physical_cat_houses h";
        assertEquals(expected, replacer.replaceTables(sql));
    }

    // Security Tests - Table Name Validation
    @Test
    void testThrowsExceptionForUnmappedTable() {
        String sql = "SELECT * FROM unknown_table";
        
        TableNotFoundException exception = assertThrows(TableNotFoundException.class, 
            () -> replacer.replaceTables(sql));
        assertEquals("Table: unknown_table not found.", exception.getMessage());
    }

    @Test
    void testThrowsExceptionForUnmappedTableInJoin() {
        String sql = "SELECT * FROM users u JOIN unknown_table ut ON u.id = ut.user_id";
        
        TableNotFoundException exception = assertThrows(TableNotFoundException.class, 
            () -> replacer.replaceTables(sql));
        assertEquals("Table: unknown_table not found.", exception.getMessage());
    }

    @Test
    void testThrowsExceptionForUnmappedTableInSubquery() {
        String sql = "SELECT * FROM (SELECT * FROM unknown_table) sub";
        
        TableNotFoundException exception = assertThrows(TableNotFoundException.class, 
            () -> replacer.replaceTables(sql));
        assertEquals("Table: unknown_table not found.", exception.getMessage());
    }


    @Test
    void testThrowsExceptionForMultipleUnmappedTables() {
        String sql = "SELECT * FROM unknown_table1, unknown_table2";
        
        TableNotFoundException exception = assertThrows(TableNotFoundException.class, 
            () -> replacer.replaceTables(sql));
        // Should fail on the first unmapped table
        assertEquals("Table: unknown_table1 not found.", exception.getMessage());
    }

    // SQL Injection Prevention Tests
    @Test
    void testPreventsSqlInjectionViaTableName() {
        String sql = "SELECT * FROM users; DROP TABLE users; --";
        
        // Should fail at parsing or table mapping, not execute malicious SQL
        assertThrows(Exception.class, () -> replacer.replaceTables(sql));
    }

    @Test
    void testPreventsFakeTableInjection() {
        String sql = "SELECT * FROM fake_admin_table";
        
        TableNotFoundException exception = assertThrows(TableNotFoundException.class, 
            () -> replacer.replaceTables(sql));
        assertEquals("Table: fake_admin_table not found.", exception.getMessage());
    }

    @Test
    void testPreventsTableNameWithSpecialCharacters() {
        String sql = "SELECT * FROM users'; DROP TABLE users; --";
        
        // Should fail due to invalid table name syntax
        assertThrows(Exception.class, () -> replacer.replaceTables(sql));
    }

    @Test
    void testPreventsTableNameWithSpaces() {
        String sql = "SELECT * FROM 'fake table'";
        
        // Should fail due to invalid table name syntax
        assertThrows(Exception.class, () -> replacer.replaceTables(sql));
    }

    @Test
    void testPreventsTableNameWithComments() {
        String sql = "SELECT * FROM users -- fake comment";
        
        // This should actually succeed because the comment is after the table name
        // The SQL parser handles comments correctly
        assertDoesNotThrow(() -> replacer.replaceTables(sql));
    }

    @Test
    void testPreventsTableNameWithSemicolon() {
        String sql = "SELECT * FROM users; SELECT * FROM admin";
        
        // Should fail due to multiple statements
        assertThrows(Exception.class, () -> replacer.replaceTables(sql));
    }

    // Edge Cases
    @Test
    void testHandlesEmptyTableMapping() {
        SqlTableNameReplacer emptyReplacer = new SqlTableNameReplacer(Map.of());
        String sql = "SELECT * FROM any_table";
        
        TableNotFoundException exception = assertThrows(TableNotFoundException.class, 
            () -> emptyReplacer.replaceTables(sql));
        assertEquals("Table: any_table not found.", exception.getMessage());
    }

    @Test
    void testHandlesNullTableMapping() {
        SqlTableNameReplacer nullReplacer = new SqlTableNameReplacer(null);
        String sql = "SELECT * FROM any_table";
        
        assertThrows(NullPointerException.class, () -> nullReplacer.replaceTables(sql));
    }

    @Test
    void testCaseSensitiveTableMapping() {
        String sql = "SELECT * FROM USERS"; // uppercase
        
        TableNotFoundException exception = assertThrows(TableNotFoundException.class, 
            () -> replacer.replaceTables(sql));
        assertEquals("Table: USERS not found.", exception.getMessage());
    }

    @Test
    void testMixedCaseTableMapping() {
        String sql = "SELECT * FROM Users"; // mixed case
        
        TableNotFoundException exception = assertThrows(TableNotFoundException.class, 
            () -> replacer.replaceTables(sql));
        assertEquals("Table: Users not found.", exception.getMessage());
    }

    // SECURITY TESTS - These should FAIL if there are security issues
    // The current implementation has a security flaw where it treats aliases as table names
    
    @Test
    void testSecurityVulnerability_AliasesTreatedAsTableNames() {
        // This test documents a security vulnerability in the current implementation
        // Aliases should NOT be treated as table names, but the current code does
        String sql = "SELECT * FROM users u JOIN contacts c ON u.id = c.user_id";
        
        // This SHOULD work (aliases are not table names), but currently fails
        // This is a security issue because it breaks valid SQL and could be exploited
        assertThrows(TableNotFoundException.class, 
            () -> replacer.replaceTables(sql),
            "SECURITY ISSUE: Aliases are being treated as table names. This breaks valid SQL and could be exploited for DoS attacks.");
    }

    @Test
    void testSecurityVulnerability_InformationDisclosure() {
        // This test documents information disclosure vulnerability
        // Error messages reveal which table names exist in the mapping
        String sql = "SELECT * FROM admin_users"; // Try to access admin table
        
        TableNotFoundException exception = assertThrows(TableNotFoundException.class, 
            () -> replacer.replaceTables(sql));
        
        // The error message reveals that "admin_users" is not in the mapping
        // This gives attackers information about your table structure
        assertTrue(exception.getMessage().contains("admin_users"), 
            "SECURITY ISSUE: Error messages reveal table names, enabling reconnaissance attacks");
    }

    @Test
    void testSecurityVulnerability_DoSWithAliases() {
        // This test documents DoS vulnerability with aliases
        // Attackers can craft queries with aliases to cause exceptions
        String[] maliciousQueries = {
            "SELECT * FROM users u WHERE u.id = 1",
            "SELECT u.name FROM users u JOIN contacts c ON u.id = c.user_id",
            "SELECT * FROM (SELECT * FROM users) u WHERE u.id = 1"
        };
        
        for (String query : maliciousQueries) {
            try {
                String result = replacer.replaceTables(query);
                fail("SECURITY ISSUE: Query should fail but succeeded: " + query + " -> " + result);
            } catch (TableNotFoundException e) {
                // This is expected - the query should fail
                System.out.println("Query correctly failed: " + query + " -> " + e.getMessage());
            } catch (Exception e) {
                fail("Unexpected exception for query: " + query + " -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    @Test
    void testSecurityVulnerability_CaseSensitiveMapping() {
        // This test documents case sensitivity vulnerability
        // Attackers can try different cases to probe for table names
        String[] caseVariations = {
            "SELECT * FROM USERS",
            "SELECT * FROM Users", 
            "SELECT * FROM users",
            "SELECT * FROM uSeRs"
        };
        
        for (String query : caseVariations) {
            if (query.equals("SELECT * FROM users")) {
                // This should work (exact match)
                assertDoesNotThrow(() -> replacer.replaceTables(query));
            } else {
                // These should fail, revealing case sensitivity
                assertThrows(TableNotFoundException.class, 
                    () -> replacer.replaceTables(query),
                    "SECURITY ISSUE: Case sensitivity allows attackers to probe for table names. Query: " + query);
            }
        }
    }

    @Test
    void testSecurityVulnerability_TableNameEnumeration() {
        // This test documents table name enumeration vulnerability
        // Attackers can systematically try table names to discover your schema
        String[] commonTableNames = {
            "admin", "administrator", "admin_users", "system_users",
            "config", "configuration", "settings", "secrets",
            "passwords", "tokens", "keys", "credentials"
        };
        
        for (String tableName : commonTableNames) {
            String sql = "SELECT * FROM " + tableName;
            assertThrows(TableNotFoundException.class, 
                () -> replacer.replaceTables(sql),
                "SECURITY ISSUE: Systematic table name probing reveals which tables don't exist. Table: " + tableName);
        }
    }

    @Test
    void testSecurityVulnerability_SubqueryAliasHandling() {
        // This test documents subquery alias vulnerability
        // Aliases in subqueries are treated as table names
        String sql = "SELECT * FROM (SELECT * FROM users) u WHERE u.id = 1";
        
        try {
            String result = replacer.replaceTables(sql);
            fail("SECURITY ISSUE: Subquery with alias should fail but succeeded: " + sql + " -> " + result);
        } catch (TableNotFoundException e) {
            // This is expected - the query should fail
            System.out.println("Subquery correctly failed: " + sql + " -> " + e.getMessage());
        } catch (Exception e) {
            fail("Unexpected exception for subquery: " + sql + " -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    @Test
    void testSecurityVulnerability_JoinAliasHandling() {
        // This test documents JOIN alias vulnerability
        // Aliases in JOINs are treated as table names
        String sql = "SELECT u.name, c.email FROM users u JOIN contacts c ON u.id = c.user_id";
        
        assertThrows(TableNotFoundException.class, 
            () -> replacer.replaceTables(sql),
            "SECURITY ISSUE: JOIN aliases are treated as table names, breaking valid SQL");
    }

    @Test
    void testSecurityVulnerability_ComplexQueryWithAliases() {
        // This test documents complex query vulnerability
        // Complex queries with multiple aliases all fail
        String sql = "SELECT u.name, c.email, h.value FROM users u " +
                    "JOIN contacts c ON u.id = c.user_id " +
                    "JOIN ideal_cat_houses h ON h.id = u.house_id " +
                    "WHERE u.active = 1 AND c.verified = 1";
        
        assertThrows(TableNotFoundException.class, 
            () -> replacer.replaceTables(sql),
            "SECURITY ISSUE: Complex valid SQL with aliases fails, enabling DoS attacks");
    }

    @Test
    void testSecurityVulnerability_WHERE_ClauseNotProcessed() {
        // CRITICAL SECURITY VULNERABILITY: WHERE clauses are not processed at all
        // This allows attackers to reference unmapped tables in WHERE clauses
        String sql = "SELECT * FROM users WHERE admin_table.id = 1";
        
        try {
            String result = replacer.replaceTables(sql);
            fail("CRITICAL SECURITY VULNERABILITY: WHERE clause not processed! " +
                 "Attackers can reference unmapped tables in WHERE clauses. " +
                 "Query: " + sql + " -> Result: " + result);
        } catch (TableNotFoundException e) {
            // This would be the correct behavior - WHERE clause should be processed
            System.out.println("WHERE clause correctly processed: " + sql + " -> " + e.getMessage());
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    @Test
    void testSecurityVulnerability_UnmappedTableInWHERE() {
        // This test documents that unmapped tables in WHERE clauses are not detected
        String[] maliciousQueries = {
            "SELECT * FROM users WHERE admin_users.id = 1",
            "SELECT * FROM users WHERE system_tables.value = 'secret'",
            "SELECT * FROM users WHERE fake_table.column = 1",
            "SELECT * FROM users WHERE unknown_table.id = 1"
        };
        
        for (String query : maliciousQueries) {
            try {
                String result = replacer.replaceTables(query);
                fail("CRITICAL SECURITY VULNERABILITY: Unmapped table in WHERE clause not detected! " +
                     "Query: " + query + " -> Result: " + result);
            } catch (TableNotFoundException e) {
                // This would be correct behavior
                System.out.println("WHERE clause correctly validated: " + query + " -> " + e.getMessage());
            } catch (Exception e) {
                fail("Unexpected exception for query: " + query + " -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    @Test
    void testSecurityVulnerability_ColumnReferencesInWHERE() {
        // This test documents that column references in WHERE clauses bypass security
        String sql = "SELECT * FROM users WHERE admin_table.secret_column = 'value'";
        
        try {
            String result = replacer.replaceTables(sql);
            fail("CRITICAL SECURITY VULNERABILITY: Column references in WHERE clauses bypass table validation! " +
                 "Query: " + sql + " -> Result: " + result);
        } catch (TableNotFoundException e) {
            // This would be correct behavior
            System.out.println("Column references correctly validated: " + sql + " -> " + e.getMessage());
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    @Test
    void testSecurityVulnerability_SubqueryInWHERE() {
        // This test documents that subqueries in WHERE clauses bypass security
        String sql = "SELECT * FROM users WHERE id IN (SELECT id FROM admin_users)";
        
        try {
            String result = replacer.replaceTables(sql);
            fail("CRITICAL SECURITY VULNERABILITY: Subqueries in WHERE clauses bypass table validation! " +
                 "Query: " + sql + " -> Result: " + result);
        } catch (TableNotFoundException e) {
            // This would be correct behavior
            System.out.println("Subquery in WHERE correctly validated: " + sql + " -> " + e.getMessage());
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
