package com.aidb.aidb_backend.controller;

import com.aidb.aidb_backend.exception.IllegalSqlException;
import com.aidb.aidb_backend.exception.OpenAiApiException;
import com.aidb.aidb_backend.exception.http.ForbiddenException;
import com.aidb.aidb_backend.exception.http.HttpException;
import com.aidb.aidb_backend.model.dto.QueryDto;
import com.aidb.aidb_backend.model.firestore.Query;
import com.aidb.aidb_backend.security.authorization.FirebaseAuthService;
import com.aidb.aidb_backend.service.api.QueryTranslatorService;
import com.aidb.aidb_backend.service.util.sql.UserQueryProcessorService;
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
    private QueryTranslatorService queryTranslatorService;

    @Autowired
    private UserQueryProcessorService userQueryProcessorService;

    @Autowired
    private FirebaseAuthService firebaseAuthService;

    private static final Logger logger = LoggerFactory.getLogger(QueryController.class);

    @PostMapping("/translate")
    public ResponseEntity<String> generateSql(@RequestBody String nlQuery) {
        try {
            String userId = firebaseAuthService.getUserIdPlaceholder();
            Query sqlQuery = queryTranslatorService.translateToSql(userId, nlQuery);
            return ResponseEntity.ok(sqlQuery.getSqlQuery());
        } catch (OpenAiApiException e) {
            logger.error(e.getMessage(), e.getHttpStatus());
            return new ResponseEntity<>(e.getMessage(), e.getHttpStatus());
        } catch (Exception e) {
            logger.error(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            return new ResponseEntity<>("Unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<Object> executeSql(@RequestBody String sqlQuery) {
        try {
            System.out.println(sqlQuery);
            String userId = firebaseAuthService.getUserIdPlaceholder();
            List<Map<String, Object>> queryResult = userQueryProcessorService.executeSafeSelectQuery(sqlQuery);
            return ResponseEntity.ok(queryResult);
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
            // TODO - Now - Remove
            System.out.println("Authenticated user: " + userId);
            List<QueryDto> queries = queryTranslatorService.getAllQueryDtos(userId);
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
    public ResponseEntity<Object> getQueryById(@PathVariable String id) {
        try {
            String userId = firebaseAuthService.getUserIdPlaceholder();
            Query query = queryTranslatorService.getQueryById(id, userId);
            return ResponseEntity.ok(query);
        } catch (ForbiddenException e) {
            logger.error(e.getMessage(), e.getHttpStatus());
            return new ResponseEntity<>(e.getMessage(), e.getHttpStatus());
        } catch (Exception e) {
            logger.error(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            return new ResponseEntity<>("Unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


}
