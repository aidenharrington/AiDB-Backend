package com.aidb.aidb_backend.orchestrator;

import com.aidb.aidb_backend.exception.http.ForbiddenException;
import com.aidb.aidb_backend.model.dto.QueryDto;
import com.aidb.aidb_backend.model.firestore.Query;
import com.aidb.aidb_backend.model.firestore.Status;
import com.aidb.aidb_backend.service.api.OpenAiClient;
import com.aidb.aidb_backend.service.database.firestore.QueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.util.List;
import java.util.concurrent.ExecutionException;

@Component
public class QueryTranslatorOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(QueryTranslatorOrchestrator.class);

    private final QueryService queryService;
    private final OpenAiClient openAiClient;

    @Autowired
    public QueryTranslatorOrchestrator(
            QueryService queryService, OpenAiClient openAiClient) {
        this.queryService = queryService;
       this.openAiClient = openAiClient;
    }

    public Query translateToSql(String userId, Query query) {
        String sqlQuery = openAiClient.getSqlTranslation(query.getNlQuery());
        query.setSqlQuery(sqlQuery);
        query.setStatus(Status.TRANSLATED);

        saveQueryGracefully(query);

        return query;
    }

    public List<QueryDto> getAllQueryDtos(String userId) throws ExecutionException, InterruptedException {
        return queryService.getAllQueryDtos(userId);
    }

    public Query getQueryById(String queryId, String userId) throws ExecutionException, InterruptedException {
        Query query = queryService.getQueryById(queryId);

        if (query != null && query.getUserId().equalsIgnoreCase(userId)) {
            return query;
        } else {
            throw new ForbiddenException("Not authorized to access query");
        }
    }

    private void saveQueryGracefully(Query query) {
        try {
            queryService.addQuery(query);
        } catch (Exception e) {
            logger.error("{}{}", "Failed saving query gracefully.", e.getMessage());
        }
    }

}
