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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QueryTranslatorOrchestratorTest {

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

    @Test
    void translateToSql_success_setsSqlAndStatus_andSavesGracefully() throws Exception {
        Query query = new Query();
        query.setNlQuery("get all users");
        when(openAiClient.getSqlTranslation("get all users")).thenReturn("SELECT * FROM users");

        QueryDTO result = orchestrator.translateToSql("user-1", query);

        assertEquals("user-1", result.getUserId());
        assertEquals("SELECT * FROM users", result.getSqlQuery());
        assertEquals(Status.TRANSLATED, result.getStatus());
        verify(queryService, times(1)).addQuery(any(Query.class));
    }

    @Test
    void translateToSql_openAiThrows_doesNotSave_andPropagates() throws Exception {
        Query query = new Query();
        query.setNlQuery("bad");
        when(openAiClient.getSqlTranslation("bad")).thenThrow(new OpenAiApiException("boom", null));

        assertThrows(OpenAiApiException.class, () -> orchestrator.translateToSql("user-1", query));
        verify(queryService, never()).addQuery(any());
    }

    @Test
    void translateToSql_saveGracefully_swallowsExceptions() throws Exception {
        Query query = new Query();
        query.setNlQuery("ok");
        when(openAiClient.getSqlTranslation("ok")).thenReturn("SELECT 1");
        doThrow(new RuntimeException("fail")).when(queryService).addQuery(any(Query.class));

        QueryDTO result = orchestrator.translateToSql("user-1", query);

        assertEquals("SELECT 1", result.getSqlQuery());
        assertEquals(Status.TRANSLATED, result.getStatus());
        verify(queryService, times(1)).addQuery(any(Query.class));
    }

    @Test
    void getAllQueryDTOs_delegatesToService() throws Exception {
        when(queryService.getAllQueryDtos("user-1", "1234")).thenReturn(List.of(mock(QueryDTO.class)));
        var dtos = orchestrator.getAllQueryDTOs("user-1", "1234");
        assertEquals(1, dtos.size());
        verify(queryService).getAllQueryDtos("user-1", "1234");
    }

    @Test
    void getQueryById_allowsOwner() throws Exception {
        Query q = new Query();
        q.setUserId("user-1");
        when(queryService.getQueryById("q1")).thenReturn(q);
        QueryDTO result = orchestrator.getQueryById("user-1", "q1");
        assertEquals("user-1", result.getUserId());
    }

    @Test
    void getQueryById_forbiddenWhenDifferentUser() throws Exception {
        Query q = new Query();
        q.setUserId("other");
        when(queryService.getQueryById("q1")).thenReturn(q);
        assertThrows(ForbiddenException.class, () -> orchestrator.getQueryById("user-1", "q1"));
    }

    @Test
    void getQueryById_forbiddenWhenNullQuery() throws Exception {
        when(queryService.getQueryById("q1")).thenReturn(null);
        assertThrows(ForbiddenException.class, () -> orchestrator.getQueryById("user-1", "q1"));
    }
}
