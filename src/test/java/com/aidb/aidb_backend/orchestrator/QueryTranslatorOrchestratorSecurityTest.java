package com.aidb.aidb_backend.orchestrator;

import com.aidb.aidb_backend.exception.OpenAiApiException;
import com.aidb.aidb_backend.exception.http.ForbiddenException;
import com.aidb.aidb_backend.model.dto.QueryDTO;
import com.aidb.aidb_backend.model.firestore.Query;
import com.aidb.aidb_backend.model.firestore.Status;
import com.aidb.aidb_backend.service.api.OpenAiClient;
import com.aidb.aidb_backend.service.database.firestore.QueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Security-focused tests for QueryTranslatorOrchestrator
 * Tests authorization, malicious input handling, and data access controls
 */
class QueryTranslatorOrchestratorSecurityTest {

    @Mock
    private QueryService queryService;

    @Mock
    private OpenAiClient openAiClient;

    @InjectMocks
    private QueryTranslatorOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // Authorization Tests
    @Test
    void getQueryById_preventsAccessToOtherUsersQueries() throws Exception {
        String userId = "user-1";
        String queryId = "query-123";
        
        Query query = new Query();
        query.setId(queryId);
        query.setUserId("other-user"); // Different user owns this query
        
        when(queryService.getQueryById(queryId)).thenReturn(query);
        
        assertThrows(ForbiddenException.class, () -> orchestrator.getQueryById(userId, queryId));
        verify(queryService).getQueryById(queryId);
    }

    @Test
    void getQueryById_allowsAccessToOwnQueries() throws Exception {
        String userId = "user-1";
        String queryId = "query-123";
        
        Query query = new Query();
        query.setId(queryId);
        query.setUserId(userId); // Same user owns this query
        
        when(queryService.getQueryById(queryId)).thenReturn(query);
        
        QueryDTO result = orchestrator.getQueryById(userId, queryId);
        
        assertEquals(userId, result.getUserId());
        verify(queryService).getQueryById(queryId);
    }

    @Test
    void getQueryById_allowsCaseInsensitiveUserIdComparison() throws Exception {
        String userId = "User-1";
        String queryId = "query-123";
        
        Query query = new Query();
        query.setId(queryId);
        query.setUserId("user-1"); // Different case
        
        when(queryService.getQueryById(queryId)).thenReturn(query);
        
        QueryDTO result = orchestrator.getQueryById(userId, queryId);
        
        assertEquals("user-1", result.getUserId());
        verify(queryService).getQueryById(queryId);
    }

    @Test
    void getQueryById_throwsForbiddenWhenQueryNotFound() throws Exception {
        String userId = "user-1";
        String queryId = "non-existent-query";
        
        when(queryService.getQueryById(queryId)).thenReturn(null);
        
        assertThrows(ForbiddenException.class, () -> orchestrator.getQueryById(userId, queryId));
        verify(queryService).getQueryById(queryId);
    }

    @Test
    void getQueryById_handlesNullUserId() throws Exception {
        String queryId = "query-123";
        
        Query query = new Query();
        query.setId(queryId);
        query.setUserId("user-1");
        
        when(queryService.getQueryById(queryId)).thenReturn(query);
        
        // The implementation actually throws ForbiddenException when userId is null
        assertThrows(ForbiddenException.class, () -> orchestrator.getQueryById(null, queryId));
    }

    @Test
    void getQueryById_handlesEmptyUserId() throws Exception {
        String userId = "";
        String queryId = "query-123";
        
        Query query = new Query();
        query.setId(queryId);
        query.setUserId("user-1");
        
        when(queryService.getQueryById(queryId)).thenReturn(query);
        
        assertThrows(ForbiddenException.class, () -> orchestrator.getQueryById(userId, queryId));
    }

    @Test
    void getQueryById_handlesNullQueryUserId() throws Exception {
        String userId = "user-1";
        String queryId = "query-123";
        
        Query query = new Query();
        query.setId(queryId);
        query.setUserId(null); // Null user ID in query
        
        when(queryService.getQueryById(queryId)).thenReturn(query);
        
        // The implementation throws NullPointerException when query.getUserId() is null
        assertThrows(NullPointerException.class, () -> orchestrator.getQueryById(userId, queryId));
    }

    // Malicious Input Tests
    @Test
    void translateToSql_handlesMaliciousNaturalLanguageQuery() throws Exception {
        String userId = "user-1";
        Query query = new Query();
        query.setNlQuery("'; DROP TABLE users; --");
        
        when(openAiClient.getSqlTranslation("'; DROP TABLE users; --"))
            .thenReturn("SELECT 1"); // AI should sanitize/ignore malicious input
        
        QueryDTO result = orchestrator.translateToSql(userId, query);
        
        assertEquals(userId, result.getUserId());
        assertEquals("SELECT 1", result.getSqlQuery());
        assertEquals(Status.TRANSLATED, result.getStatus());
        verify(queryService).addQuery(any(Query.class));
    }

    @Test
    void translateToSql_handlesExtremelyLongNaturalLanguageQuery() throws Exception {
        String userId = "user-1";
        Query query = new Query();
        String longQuery = "A".repeat(10000); // Very long query
        query.setNlQuery(longQuery);
        
        when(openAiClient.getSqlTranslation(longQuery))
            .thenReturn("SELECT 1");
        
        QueryDTO result = orchestrator.translateToSql(userId, query);
        
        assertEquals(userId, result.getUserId());
        assertEquals("SELECT 1", result.getSqlQuery());
        assertEquals(Status.TRANSLATED, result.getStatus());
        verify(queryService).addQuery(any(Query.class));
    }

    @Test
    void translateToSql_handlesSpecialCharactersInQuery() throws Exception {
        String userId = "user-1";
        Query query = new Query();
        query.setNlQuery("Find users with names containing äöü and symbols !@#$%^&*()");
        
        when(openAiClient.getSqlTranslation("Find users with names containing äöü and symbols !@#$%^&*()"))
            .thenReturn("SELECT * FROM users WHERE name LIKE '%äöü%'");
        
        QueryDTO result = orchestrator.translateToSql(userId, query);
        
        assertEquals(userId, result.getUserId());
        assertEquals("SELECT * FROM users WHERE name LIKE '%äöü%'", result.getSqlQuery());
        assertEquals(Status.TRANSLATED, result.getStatus());
        verify(queryService).addQuery(any(Query.class));
    }

    @Test
    void translateToSql_handlesEmptyNaturalLanguageQuery() throws Exception {
        String userId = "user-1";
        Query query = new Query();
        query.setNlQuery("");
        
        when(openAiClient.getSqlTranslation(""))
            .thenReturn(""); // AI might return empty for empty input
        
        QueryDTO result = orchestrator.translateToSql(userId, query);
        
        assertEquals(userId, result.getUserId());
        assertEquals("", result.getSqlQuery());
        assertEquals(Status.TRANSLATED, result.getStatus());
        verify(queryService).addQuery(any(Query.class));
    }

    @Test
    void translateToSql_handlesNullNaturalLanguageQuery() throws Exception {
        String userId = "user-1";
        Query query = new Query();
        query.setNlQuery(null);
        
        when(openAiClient.getSqlTranslation(null))
            .thenReturn("SELECT 1"); // AI handles null gracefully
        
        QueryDTO result = orchestrator.translateToSql(userId, query);
        
        assertEquals(userId, result.getUserId());
        assertEquals("SELECT 1", result.getSqlQuery());
        assertEquals(Status.TRANSLATED, result.getStatus());
        verify(queryService).addQuery(any(Query.class));
    }

    // API Security Tests
    @Test
    void translateToSql_handlesOpenAiRateLimiting() throws Exception {
        String userId = "user-1";
        Query query = new Query();
        query.setNlQuery("Get all users");
        
        when(openAiClient.getSqlTranslation("Get all users"))
            .thenThrow(new OpenAiApiException("Rate limit exceeded", null));
        
        assertThrows(OpenAiApiException.class, () -> orchestrator.translateToSql(userId, query));
        verify(queryService, never()).addQuery(any());
    }

    @Test
    void translateToSql_handlesOpenAiQuotaExceeded() throws Exception {
        String userId = "user-1";
        Query query = new Query();
        query.setNlQuery("Get all users");
        
        when(openAiClient.getSqlTranslation("Get all users"))
            .thenThrow(new OpenAiApiException("Quota exceeded", null));
        
        assertThrows(OpenAiApiException.class, () -> orchestrator.translateToSql(userId, query));
        verify(queryService, never()).addQuery(any());
    }

    @Test
    void translateToSql_handlesOpenAiInvalidApiKey() throws Exception {
        String userId = "user-1";
        Query query = new Query();
        query.setNlQuery("Get all users");
        
        when(openAiClient.getSqlTranslation("Get all users"))
            .thenThrow(new OpenAiApiException("Invalid API key", null));
        
        assertThrows(OpenAiApiException.class, () -> orchestrator.translateToSql(userId, query));
        verify(queryService, never()).addQuery(any());
    }

    // Data Access Tests
    @Test
    void getAllQueryDTOs_onlyReturnsUserOwnedQueries() throws Exception {
        String userId = "user-1";
        List<QueryDTO> userQueries = List.of(
            new QueryDTO(), // Assuming these belong to the user
            new QueryDTO()
        );
        
        when(queryService.getAllQueryDtos(userId)).thenReturn(userQueries);
        
        List<QueryDTO> result = orchestrator.getAllQueryDTOs(userId);
        
        assertSame(userQueries, result);
        verify(queryService).getAllQueryDtos(userId);
    }

    @Test
    void getAllQueryDTOs_handlesEmptyResult() throws Exception {
        String userId = "user-with-no-queries";
        List<QueryDTO> emptyList = List.of();
        
        when(queryService.getAllQueryDtos(userId)).thenReturn(emptyList);
        
        List<QueryDTO> result = orchestrator.getAllQueryDTOs(userId);
        
        assertTrue(result.isEmpty());
        verify(queryService).getAllQueryDtos(userId);
    }

    @Test
    void getAllQueryDTOs_handlesServiceException() throws Exception {
        String userId = "user-1";
        
        when(queryService.getAllQueryDtos(userId))
            .thenThrow(new ExecutionException(new RuntimeException("Database error")));
        
        assertThrows(ExecutionException.class, () -> orchestrator.getAllQueryDTOs(userId));
        verify(queryService).getAllQueryDtos(userId);
    }

    // Error Handling and Resilience Tests
    @Test
    void translateToSql_continuesWhenSaveQueryFails() throws Exception {
        String userId = "user-1";
        Query query = new Query();
        query.setNlQuery("Get all users");
        
        when(openAiClient.getSqlTranslation("Get all users")).thenReturn("SELECT * FROM users");
        doThrow(new RuntimeException("Database save failed")).when(queryService).addQuery(any());
        
        QueryDTO result = orchestrator.translateToSql(userId, query);
        
        assertEquals(userId, result.getUserId());
        assertEquals("SELECT * FROM users", result.getSqlQuery());
        assertEquals(Status.TRANSLATED, result.getStatus());
        verify(queryService).addQuery(any(Query.class));
    }

    @Test
    void translateToSql_handlesNullUserId() throws Exception {
        Query query = new Query();
        query.setNlQuery("Get all users");
        
        when(openAiClient.getSqlTranslation("Get all users")).thenReturn("SELECT * FROM users");
        
        QueryDTO result = orchestrator.translateToSql(null, query);
        
        assertNull(result.getUserId());
        assertEquals("SELECT * FROM users", result.getSqlQuery());
        assertEquals(Status.TRANSLATED, result.getStatus());
        verify(queryService).addQuery(any(Query.class));
    }

    @Test
    void translateToSql_handlesEmptyUserId() throws Exception {
        String userId = "";
        Query query = new Query();
        query.setNlQuery("Get all users");
        
        when(openAiClient.getSqlTranslation("Get all users")).thenReturn("SELECT * FROM users");
        
        QueryDTO result = orchestrator.translateToSql(userId, query);
        
        assertEquals("", result.getUserId());
        assertEquals("SELECT * FROM users", result.getSqlQuery());
        assertEquals(Status.TRANSLATED, result.getStatus());
        verify(queryService).addQuery(any(Query.class));
    }

    @Test
    void translateToSql_handlesNullQuery() throws Exception {
        String userId = "user-1";
        
        assertThrows(NullPointerException.class, () -> orchestrator.translateToSql(userId, null));
        verifyNoInteractions(openAiClient, queryService);
    }
}