package com.aidb.aidb_backend.controller;

import com.aidb.aidb_backend.model.api.APIResponse;
import com.aidb.aidb_backend.model.dto.QueryDTO;
import com.aidb.aidb_backend.model.firestore.Query;
import com.aidb.aidb_backend.model.firestore.util.LimitedOperation;
import com.aidb.aidb_backend.security.authorization.FirebaseAuthService;
import com.aidb.aidb_backend.orchestrator.QueryTranslatorOrchestrator;
import com.aidb.aidb_backend.orchestrator.QueryExecutionOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/queries")
public class QueryController extends BaseController {

    @Autowired
    private QueryTranslatorOrchestrator queryTranslatorOrchestrator;

    @Autowired
    private QueryExecutionOrchestrator queryExecutionOrchestrator;

    @Autowired
    private FirebaseAuthService firebaseAuthService;

    private static final Logger logger = LoggerFactory.getLogger(QueryController.class);

    @PostMapping("/translate")
    public ResponseEntity<APIResponse<QueryDTO>> translateToSql(@RequestHeader("Authorization") String authToken, @RequestBody Query query) throws Exception {
        return handleRequestWithLimit(authToken,
                LimitedOperation.TRANSLATION,
                1,
                (userId, args) ->
                        queryTranslatorOrchestrator.translateToSql(userId, query), query
        );
    }

    @PostMapping
    public ResponseEntity<APIResponse<List<Map<String, Object>>>> executeSql(@RequestHeader("Authorization") String authToken, @RequestBody QueryDTO query) throws Exception {
        return handleRequestWithLimit(authToken,
                LimitedOperation.QUERY,
                1,
                (userId, args) ->
                        queryExecutionOrchestrator.executeSafeSelectQuery(userId, query), query
        );
    }

    @GetMapping
    public ResponseEntity<APIResponse<List<QueryDTO>>> getAllQueries(@RequestHeader("Authorization") String authToken,
                                                                     @RequestParam String projectId) throws Exception {
        return handleRequest(authToken,
                (userId, args) -> queryTranslatorOrchestrator.getAllQueryDTOs(userId, projectId), projectId
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<QueryDTO>> getQueryById(@RequestHeader("Authorization") String authToken, @PathVariable String id) throws Exception {
        return handleRequest(authToken,
                (userId, args) ->
                        queryTranslatorOrchestrator.getQueryById(userId, id), id
        );
    }


}
