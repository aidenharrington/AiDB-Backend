package com.aidb.aidb_backend.controller;

import com.aidb.aidb_backend.config.FirebaseConfig;
import com.aidb.aidb_backend.exception.OpenAiApiException;
import com.aidb.aidb_backend.model.firestore.Query;
import com.aidb.aidb_backend.security.authorization.FirebaseAuthService;
import com.aidb.aidb_backend.service.api.QueryTranslatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/query-translator")
public class QueryTranslatorController {

    @Autowired
    private QueryTranslatorService queryTranslatorService;

    @Autowired
    private FirebaseAuthService firebaseAuthService;

    private static final Logger logger = LoggerFactory.getLogger(QueryTranslatorController.class);

    @PostMapping("/translate")
    public ResponseEntity<String> generateSql(@RequestBody String nlQuery) {
        try {
            String userId = firebaseAuthService.getUserIdPlaceholder();
            Query sqlQuery = queryTranslatorService.translateToSql(nlQuery, userId);
            return ResponseEntity.ok(sqlQuery.getSqlQuery());
        } catch (OpenAiApiException e) {
            logger.error(e.getMessage(), e.getHttpStatus());
            return new ResponseEntity<>(e.getMessage(), e.getHttpStatus());
        } catch (Exception e) {
            logger.error(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            return new ResponseEntity<>("Unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
