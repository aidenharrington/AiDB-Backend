package com.aidb.aidb_backend.service.util.sql;

import net.sf.jsqlparser.JSQLParserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void testMixedCaseKeywordsAndExtraWhitespace() throws JSQLParserException {
        String sql = "SELECT  *   FrOm   ideal_cat_houses   H  JOIN ideal_cat_number  N  ON H.id = N.house_id";
        String expected = "SELECT * FROM physical_cat_houses H JOIN physical_cat_number N ON H.id = N.house_id";
        assertEquals(expected, replacer.replaceTables(sql));
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
    void testMultipleJoins() throws JSQLParserException {
        String sql = "SELECT * FROM users u JOIN contacts c ON u.id=c.user_id JOIN ideal_cat_houses h ON h.id=u.house_id";
        String expected = "SELECT * FROM app_users u JOIN app_contacts c ON u.id = c.user_id JOIN physical_cat_houses h ON h.id = u.house_id";
        assertEquals(expected, replacer.replaceTables(sql));
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
    void testAliasedSubqueryWithJoin() throws JSQLParserException {
        String sql = "SELECT s.id FROM (SELECT * FROM ideal_cat_houses) s JOIN ideal_cat_number n ON s.id=n.house_id";
        String expected = "SELECT s.id FROM (SELECT * FROM physical_cat_houses) s JOIN physical_cat_number n ON s.id = n.house_id";
        assertEquals(expected, replacer.replaceTables(sql));
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
}
