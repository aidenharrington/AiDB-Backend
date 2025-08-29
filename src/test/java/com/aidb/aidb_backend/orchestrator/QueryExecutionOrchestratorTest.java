package com.aidb.aidb_backend.orchestrator;

import com.aidb.aidb_backend.exception.IllegalSqlException;
import com.aidb.aidb_backend.exception.ProjectNotFoundException;
import com.aidb.aidb_backend.model.dto.QueryDTO;
import com.aidb.aidb_backend.model.firestore.Query;
import com.aidb.aidb_backend.model.firestore.Status;
import com.aidb.aidb_backend.service.database.firestore.QueryService;
import com.aidb.aidb_backend.service.database.postgres.TableMetadataService;
import com.aidb.aidb_backend.service.database.postgres.user_created_tables.UserQueryDataService;
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

    @Mock
    private TableMetadataService tableMetadataService;

    @InjectMocks
    private QueryExecutionOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void executeSafeSelectQuery_success_executesAndSavesGracefully() throws Exception {
        QueryDTO queryDTO = new QueryDTO();
        queryDTO.setSqlQuery("SELECT 1");
        queryDTO.setProjectId("123");
        
        Map<String, String> tableMapping = Map.of("users", "user_table_123");
        when(tableMetadataService.getTableNameMapping("user-1", 123L)).thenReturn(tableMapping);
        when(userQueryDataService.executeSql("SELECT 1")).thenReturn(List.of(Map.of("?column?", 1)));

        List<Map<String, Object>> result = orchestrator.executeSafeSelectQuery("user-1", queryDTO);

        assertEquals(1, result.size());
        verify(queryService, times(1)).addOrUpdateQuery(any(Query.class));
    }

    @Test
    void executeSafeSelectQuery_throwsOnNonSelect() throws Exception {
        QueryDTO queryDTO = new QueryDTO();
        queryDTO.setSqlQuery("UPDATE users SET name='x'");
        queryDTO.setProjectId("123");
        
        Map<String, String> tableMapping = Map.of("users", "user_table_123");
        when(tableMetadataService.getTableNameMapping("user-1", 123L)).thenReturn(tableMapping);
        
        IllegalSqlException ex = assertThrows(IllegalSqlException.class, () -> orchestrator.executeSafeSelectQuery("user-1", queryDTO));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        verifyNoInteractions(userQueryDataService);
        verify(queryService, never()).addOrUpdateQuery(any());
    }

    @Test
    void executeSafeSelectQuery_throwsOnInvalidSyntax() throws Exception {
        QueryDTO queryDTO = new QueryDTO();
        queryDTO.setSqlQuery("SELECT FROM");
        queryDTO.setProjectId("123");
        
        Map<String, String> tableMapping = Map.of("users", "user_table_123");
        when(tableMetadataService.getTableNameMapping("user-1", 123L)).thenReturn(tableMapping);
        
        IllegalSqlException ex = assertThrows(IllegalSqlException.class, () -> orchestrator.executeSafeSelectQuery("user-1", queryDTO));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        verifyNoInteractions(userQueryDataService);
        verify(queryService, never()).addOrUpdateQuery(any());
    }

    @Test
    void executeSafeSelectQuery_disallowsMultipleStatementsAndComments() throws Exception {
        QueryDTO queryDTO1 = new QueryDTO();
        queryDTO1.setSqlQuery("SELECT 1; SELECT 2");
        queryDTO1.setProjectId("123");
        
        Map<String, String> tableMapping = Map.of("users", "user_table_123");
        when(tableMetadataService.getTableNameMapping("user-1", 123L)).thenReturn(tableMapping);
        
        assertThrows(IllegalSqlException.class, () -> orchestrator.executeSafeSelectQuery("user-1", queryDTO1));

        QueryDTO queryDTO2 = new QueryDTO();
        queryDTO2.setSqlQuery("SELECT 1 -- drop table");
        queryDTO2.setProjectId("123");
        assertThrows(IllegalSqlException.class, () -> orchestrator.executeSafeSelectQuery("user-1", queryDTO2));
    }

    @Test
    void executeSafeSelectQuery_saveGracefully_swallowsExceptions() throws Exception {
        QueryDTO queryDTO = new QueryDTO();
        queryDTO.setSqlQuery("SELECT 1");
        queryDTO.setProjectId("123");
        
        Map<String, String> tableMapping = Map.of("users", "user_table_123");
        when(tableMetadataService.getTableNameMapping("user-1", 123L)).thenReturn(tableMapping);
        doThrow(new RuntimeException("fail")).when(queryService).addOrUpdateQuery(any(Query.class));
        when(userQueryDataService.executeSql("SELECT 1")).thenReturn(List.of());

        List<Map<String, Object>> result = orchestrator.executeSafeSelectQuery("user-1", queryDTO);

        assertNotNull(result);
        verify(queryService, times(1)).addOrUpdateQuery(any(Query.class));
    }

    @Test
    void executeSafeSelectQuery_throwsProjectNotFoundWhenNoTableMapping() throws Exception {
        QueryDTO queryDTO = new QueryDTO();
        queryDTO.setSqlQuery("SELECT 1");
        queryDTO.setProjectId("999");
        
        when(tableMetadataService.getTableNameMapping("user-1", 999L)).thenReturn(null);
        
        assertThrows(ProjectNotFoundException.class, () -> orchestrator.executeSafeSelectQuery("user-1", queryDTO));
        verifyNoInteractions(userQueryDataService);
        verify(queryService, never()).addOrUpdateQuery(any());
    }

    @Test
    void executeSafeSelectQuery_throwsProjectNotFoundWhenEmptyTableMapping() throws Exception {
        QueryDTO queryDTO = new QueryDTO();
        queryDTO.setSqlQuery("SELECT 1");
        queryDTO.setProjectId("999");
        
        when(tableMetadataService.getTableNameMapping("user-1", 999L)).thenReturn(Map.of());
        
        assertThrows(ProjectNotFoundException.class, () -> orchestrator.executeSafeSelectQuery("user-1", queryDTO));
        verifyNoInteractions(userQueryDataService);
        verify(queryService, never()).addOrUpdateQuery(any());
    }
}