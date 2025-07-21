package com.aidb.aidb_backend.controller;

import com.aidb.aidb_backend.exception.IllegalSqlException;
import com.aidb.aidb_backend.exception.OpenAiApiException;
import com.aidb.aidb_backend.exception.http.HttpException;
import com.aidb.aidb_backend.model.dto.QueryDto;
import com.aidb.aidb_backend.model.firestore.Query;
import com.aidb.aidb_backend.security.authorization.FirebaseAuthService;
import com.aidb.aidb_backend.orchestrator.QueryTranslatorOrchestrator;
import com.aidb.aidb_backend.orchestrator.QueryExecutionOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/queries")
public class QueryController {

    @Autowired
    private QueryTranslatorOrchestrator queryTranslatorOrchestrator;

    @Autowired
    private QueryExecutionOrchestrator queryExecutionOrchestrator;

    @Autowired
    private FirebaseAuthService firebaseAuthService;

    private static final Logger logger = LoggerFactory.getLogger(QueryController.class);

    @PostMapping("/translate")
    public ResponseEntity<Object> translateToSql(@RequestHeader("Authorization") String authToken, @RequestBody Query query) {
        try {
            String userId = firebaseAuthService.authorizeUser(authToken);
            query.setUserId(userId);
            Query translatedQuery = queryTranslatorOrchestrator.translateToSql(userId, query);
            return ResponseEntity.ok(translatedQuery);
        } catch (HttpException e) {
            logger.error(e.getMessage(), e.getHttpStatus());
            return new ResponseEntity<>(e.getMessage(), e.getHttpStatus());
        } catch (OpenAiApiException e) {
            logger.error(e.getMessage(), e.getHttpStatus());
            return new ResponseEntity<>(e.getMessage(), e.getHttpStatus());
        } catch (Exception e) {
            logger.error(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            return new ResponseEntity<>("Unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<Object> executeSql(@RequestHeader("Authorization") String authToken, @RequestBody Query query) {
        try {
            String userId = firebaseAuthService.authorizeUser(authToken);
            query.setUserId(userId);
            List<Map<String, Object>> queryResult = queryExecutionOrchestrator.executeSafeSelectQuery(query);
            return ResponseEntity.ok(queryResult);
        } catch (HttpException e) {
            logger.error(e.getMessage(), e.getHttpStatus());
            return new ResponseEntity<>(e.getMessage(), e.getHttpStatus());
        } catch (IllegalSqlException e) {
            logger.error(e.getMessage(), e.getHttpStatus());
            return new ResponseEntity<>(e.getMessage(), e.getHttpStatus());
        } catch (Exception e) {
            logger.error(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            return new ResponseEntity<>("Unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<Object> getAllQueries(@RequestHeader("Authorization") String authToken) {
        try {
            String userId = firebaseAuthService.authorizeUser(authToken);
            List<QueryDto> queries = queryTranslatorOrchestrator.getAllQueryDtos(userId);
            return ResponseEntity.ok(queries);
        } catch (HttpException e) {
            logger.error(e.getMessage(), e.getHttpStatus());
            return new ResponseEntity<>(e.getMessage(), e.getHttpStatus());
        } catch (Exception e) {
            logger.error(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            return new ResponseEntity<>("Unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getQueryById(@RequestHeader("Authorization") String authToken, @PathVariable String id) {
        try {
            String userId = firebaseAuthService.authorizeUser(authToken);
            Query query = queryTranslatorOrchestrator.getQueryById(id, userId);
            return ResponseEntity.ok(query);
        } catch (HttpException e) {
            logger.error(e.getMessage(), e.getHttpStatus());
            return new ResponseEntity<>(e.getMessage(), e.getHttpStatus());
        } catch (Exception e) {
            logger.error(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            return new ResponseEntity<>("Unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


}
