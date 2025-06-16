package com.aidb.aidb_backend.service.api;

import com.aidb.aidb_backend.exception.OpenAiApiException;
import com.aidb.aidb_backend.exception.http.ForbiddenException;
import com.aidb.aidb_backend.model.dto.QueryDto;
import com.aidb.aidb_backend.model.firestore.Query;
import com.aidb.aidb_backend.service.database.firestore.QueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;


import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class QueryTranslatorService {

    private static final Logger logger = LoggerFactory.getLogger(QueryTranslatorService.class);

    private final QueryService queryService;
    private final String openAiKey;
    private final String openAiUrl;
    private final String openAiModel;
    private final double openAiTemp;

    @Autowired
    public QueryTranslatorService(
            QueryService queryService,
            @Value("${OPENAI_API_KEY}") String openAiKey,
            @Value("${OPENAI_API_URL}") String openAiUrl,
            @Value("${OPENAI_API_MODEL}") String openAiModel,
            @Value("${OPENAI_API_TEMPERATURE}") double openAiTemp) {
        this.queryService = queryService;
        this.openAiKey = openAiKey;
        this.openAiUrl = openAiUrl;
        this.openAiModel = openAiModel;
        this.openAiTemp = openAiTemp;
    }

    public Query translateToSql(String userId, String nlQuery) {
        Query query = new Query();
        query.setUserId(userId);
        query.setNlQuery(nlQuery);

        String sqlQuery = getOpenAiSqlTranslation(nlQuery);
        query.setSqlQuery(sqlQuery);

        saveQuery(query);

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

    private String getOpenAiSqlTranslation(String nlQuery) {
        RestTemplate restTemplate = createRestTemplate();

        // TODO - MVP: remove hardcoding
        Map<String, Object> message = Map.of(
                "role", "user",
                "content", "Translate this natural language query to SQL: " + nlQuery
        );

        Map<String, Object> requestBody = Map.of(
                "model", openAiModel,
                "messages", List.of(message),
                "temperature", openAiTemp
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(openAiUrl, HttpMethod.POST, request, Map.class);

            if (response.getBody() == null) {
                throw new OpenAiApiException("OpenAI API returned a null body", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new OpenAiApiException("No choices in OpenAI response", HttpStatus.BAD_REQUEST);
            }

            Map<String, Object> messageMap = (Map<String, Object>) choices.get(0).get("message");
            if (messageMap == null) {
                throw new OpenAiApiException("No message field in OpenAI response", HttpStatus.BAD_REQUEST);
            }

            return (String) messageMap.get("content");

        } catch (OpenAiApiException e) {
            throw e; // Propagate custom exception
        } catch (Exception e) {
            throw new OpenAiApiException("Error fetching SQL translation from OpenAI", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    private void saveQuery(Query query) {
        try {
            queryService.addQuery(query);
        } catch (Exception e) {
            logger.error("{}{}", "Failed saving query gracefully.", e.getMessage());
        }
    }

    protected RestTemplate createRestTemplate() {
        return new RestTemplate();
    }

}
