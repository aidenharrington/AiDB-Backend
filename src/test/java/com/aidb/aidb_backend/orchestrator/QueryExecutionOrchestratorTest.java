package com.aidb.aidb_backend.orchestrator;

import com.aidb.aidb_backend.exception.IllegalSqlException;
import com.aidb.aidb_backend.model.firestore.Query;
import com.aidb.aidb_backend.model.firestore.Status;
import com.aidb.aidb_backend.service.database.firestore.QueryService;
import com.aidb.aidb_backend.service.database.postgres.UserQueryDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QueryExecutionOrchestratorTest {

    @Mock
    private QueryService queryService;

    @Mock
    private UserQueryDataService userQueryDataService;

    @InjectMocks
    private QueryExecutionOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void executeSafeSelectQuery_success_executesAndSavesGracefully() throws Exception {
        Query q = new Query();
        q.setSqlQuery("SELECT 1");
        when(userQueryDataService.executeSql("SELECT 1")).thenReturn(List.of(Map.of("?column?", 1)));

        List<Map<String, Object>> result = orchestrator.executeSafeSelectQuery("user-1", q);

        assertEquals("user-1", q.getUserId());
        assertEquals(Status.EXECUTED, q.getStatus());
        assertEquals(1, result.size());
        verify(queryService, times(1)).addOrUpdateQuery(q);
    }

    @Test
    void executeSafeSelectQuery_throwsOnNonSelect() throws Exception {
        Query q = new Query();
        q.setSqlQuery("UPDATE users SET name='x'");
        IllegalSqlException ex = assertThrows(IllegalSqlException.class, () -> orchestrator.executeSafeSelectQuery("user", q));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        verifyNoInteractions(userQueryDataService);
        verify(queryService, never()).addOrUpdateQuery(any());
    }

    @Test
    void executeSafeSelectQuery_throwsOnInvalidSyntax() throws Exception {
        Query q = new Query();
        q.setSqlQuery("SELECT FROM");
        IllegalSqlException ex = assertThrows(IllegalSqlException.class, () -> orchestrator.executeSafeSelectQuery("user", q));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        verifyNoInteractions(userQueryDataService);
        verify(queryService, never()).addOrUpdateQuery(any());
    }

    @Test
    void executeSafeSelectQuery_disallowsMultipleStatementsAndComments() throws Exception {
        Query q1 = new Query();
        q1.setSqlQuery("SELECT 1; SELECT 2");
        assertThrows(IllegalSqlException.class, () -> orchestrator.executeSafeSelectQuery("user", q1));

        Query q2 = new Query();
        q2.setSqlQuery("SELECT 1 -- drop table");
        assertThrows(IllegalSqlException.class, () -> orchestrator.executeSafeSelectQuery("user", q2));
    }

    @Test
    void executeSafeSelectQuery_saveGracefully_swallowsExceptions() throws Exception {
        Query q = new Query();
        q.setSqlQuery("SELECT 1");
        doThrow(new RuntimeException("fail")).when(queryService).addOrUpdateQuery(any(Query.class));
        when(userQueryDataService.executeSql("SELECT 1")).thenReturn(List.of());

        List<Map<String, Object>> result = orchestrator.executeSafeSelectQuery("user", q);

        assertNotNull(result);
        assertEquals(Status.EXECUTED, q.getStatus());
        verify(queryService, times(1)).addOrUpdateQuery(any(Query.class));
    }
}
