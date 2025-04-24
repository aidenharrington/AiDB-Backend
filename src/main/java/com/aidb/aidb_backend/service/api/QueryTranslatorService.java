package com.aidb.aidb_backend.service.api;

import com.aidb.aidb_backend.config.FirebaseConfig;
import com.aidb.aidb_backend.model.firestore.Query;
import com.aidb.aidb_backend.service.firestore.QueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;


import java.util.List;
import java.util.Map;

@Service
public class QueryTranslatorService {

    @Autowired
    private QueryService queryService;

    @Value("${OPENAI_API_KEY}")
    private String openAiKey;

    @Value("${OPENAI_API_URL}")
    private String openAiUrl;

    @Value("${OPENAI_API_MODEL}")
    private String openAiModel;

    @Value("${OPENAI_API_TEMPERATURE}")
    private double openAiTemp;

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);


    public Query translateToSql(String userId, String nlQuery) {
        Query query = new Query();
        query.setUserId(userId);
        query.setNlQuery(nlQuery);

        String sqlQuery = getOpenAiSqlTranslation(nlQuery);
        query.setSqlQuery(sqlQuery);

        saveQuery(query);

        return query;
    }

    private String getOpenAiSqlTranslation(String nlQuery) {
        RestTemplate restTemplate = new RestTemplate();

        // TODO: remove hardcoding
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

        ResponseEntity<Map> response = restTemplate.exchange(openAiUrl, HttpMethod.POST, request, Map.class);

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
        Map<String, Object> messageMap = (Map<String, Object>) choices.get(0).get("message");
        return (String) messageMap.get("content");
    }

    private void saveQuery(Query query) {
        try {
            queryService.addQuery(query);
        } catch (Exception e) {
            logger.error("{}{}", "Failed saving query gracefully.", e.getMessage());
        }
    }

}
