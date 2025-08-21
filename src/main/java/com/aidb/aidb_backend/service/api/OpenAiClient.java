package com.aidb.aidb_backend.service.api;

import com.aidb.aidb_backend.exception.OpenAiApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class OpenAiClient {
    @Value("${OPENAI_API_KEY}")
    private String openAiKey;

    @Value("${OPENAI_API_URL}")
    private String openAiUrl;

    @Value("${OPENAI_API_MODEL}")
    private String openAiModel;

    @Value("${OPENAI_API_TEMPERATURE}")
    private double openAiTemp;

    public String getSqlTranslation(String nlQuery) {
        RestTemplate restTemplate = new RestTemplate();

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
            throw new OpenAiApiException("Error fetching SQL translation from OpenAI", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
