package com.aidb.aidb_backend.service.api;

import com.aidb.aidb_backend.exception.OpenAiApiException;
import com.aidb.aidb_backend.model.firestore.Query;
import com.aidb.aidb_backend.orchestrator.QueryTranslatorOrchestrator;
import com.aidb.aidb_backend.service.database.firestore.QueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QueryTranslatorOrchestratorTest {

    private QueryTranslatorOrchestrator queryTranslatorOrchestrator;
    private QueryService queryService;
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        queryService = mock(QueryService.class);
        restTemplate = mock(RestTemplate.class);

        // Create service instance with constructor injection
        queryTranslatorOrchestrator = new QueryTranslatorOrchestrator(
                queryService,
                "fake-key",
                "http://fake-url.com",
                "gpt-3.5-turbo",
                0.5
        ) {
            @Override
            protected RestTemplate createRestTemplate() {
                return restTemplate;
            }
        };
    }

    @Test
    void testTranslateToSql_Success() {
        // Mock the OpenAI API response
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("content", "SELECT * FROM users;");

        Map<String, Object> choice = new HashMap<>();
        choice.put("message", messageMap);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("choices", List.of(choice));

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(
                eq("http://fake-url.com"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

        // Mock successful query saving (handle exceptions)
        try {
            when(queryService.addQuery(any(Query.class))).thenReturn("mocked-id");
        } catch (ExecutionException | InterruptedException e) {
            fail("Mocking addQuery failed: " + e.getMessage());
        }

        try {
            // Call method under test
            Query result = queryTranslatorOrchestrator.translateToSql("user123", "Get all users");

            // Assertions
            assertEquals("user123", result.getUserId());
            assertEquals("Get all users", result.getNlQuery());
            assertEquals("SELECT * FROM users;", result.getSqlQuery());

            // Verify interactions
            verify(queryService, times(1)).addQuery(any(Query.class));

        } catch (ExecutionException | InterruptedException e) {
            fail("Exception should not be thrown: " + e.getMessage());
        }
    }

    @Test
    void testTranslateToSql_OpenAiApiError() {
        // Mocking the OpenAI API to return a 500 error with a null body
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);

        // Mock the RestTemplate exchange method to return the error response
        when(restTemplate.exchange(
                eq("http://fake-url.com"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

        try {
            // Call method under test
            Query result = queryTranslatorOrchestrator.translateToSql("user123", "Get all users");

            // Assertions: Assert that the query translation returned null for the SQL query
            assertNull(result.getSqlQuery(), "SQL query should be null on error");
            verify(queryService, never()).addQuery(any(Query.class));

        } catch (OpenAiApiException e) {
            // Verify that the exception is thrown and contains the correct message
            assertEquals("OpenAI API returned a null body", e.getMessage());
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getHttpStatus());
        } catch (Exception e) {
            fail("Exception should not be thrown: " + e.getMessage());
        }
    }

    @Test
    void testTranslateToSql_InvalidOpenAiResponse() {
        // Mock an invalid OpenAI API response (missing expected "choices" field)
        Map<String, Object> invalidResponseBody = new HashMap<>(); // Missing expected "choices" field
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(invalidResponseBody, HttpStatus.OK);

        when(restTemplate.exchange(
                eq("http://fake-url.com"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

        try {
            // Call method under test
            Query result = queryTranslatorOrchestrator.translateToSql("user123", "Get all users");

            // Assertions
            assertNull(result.getSqlQuery(), "SQL query should be null for invalid OpenAI response");
            verify(queryService, never()).addQuery(any(Query.class));

        } catch (OpenAiApiException e) {
            // Verify the correct exception handling for missing "choices"
            assertEquals("No choices in OpenAI response", e.getMessage());
            assertEquals(HttpStatus.BAD_REQUEST, e.getHttpStatus());
        } catch (Exception e) {
            fail("Exception should not be thrown: " + e.getMessage());
        }
    }

    @Test
    void testTranslateToSql_SaveQueryGracefullyError() {
        // Mock the OpenAI API response
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("content", "SELECT * FROM users;");

        Map<String, Object> choice = new HashMap<>();
        choice.put("message", messageMap);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("choices", List.of(choice));

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(
                eq("http://fake-url.com"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

        // Simulate exception during saving the query (mocking the behavior)
        try {
            doThrow(new ExecutionException(new Throwable("Execution failed"))).when(queryService).addQuery(any(Query.class));
        } catch (ExecutionException | InterruptedException e) {
            fail("Mocking addQuery failed: " + e.getMessage());
        }

        try {
            // Call method under test
            Query result = queryTranslatorOrchestrator.translateToSql("user123", "Get all users");

            // Assertions
            assertEquals("user123", result.getUserId());
            assertEquals("Get all users", result.getNlQuery());
            assertEquals("SELECT * FROM users;", result.getSqlQuery());

            // Ensure the exception is handled gracefully, and that `addQuery` is called once
            verify(queryService, times(1)).addQuery(any(Query.class));

        } catch (ExecutionException | InterruptedException e) {
            fail("Exception should not be thrown: " + e.getMessage());
        }
    }
}
