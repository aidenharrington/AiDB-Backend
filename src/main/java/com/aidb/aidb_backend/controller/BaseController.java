package com.aidb.aidb_backend.controller;

import com.aidb.aidb_backend.model.api.APIResponse;
import com.aidb.aidb_backend.model.api.PayloadMetadata;
import com.aidb.aidb_backend.model.api.TierInfo;
import com.aidb.aidb_backend.model.firestore.util.LimitedOperation;
import com.aidb.aidb_backend.orchestrator.LimitsOrchestrator;
import com.aidb.aidb_backend.security.authorization.FirebaseAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.function.Function;

public abstract class BaseController {

    @Autowired
    FirebaseAuthService firebaseAuthService;

    @Autowired
    LimitsOrchestrator limitsOrchestrator;

    private static final Logger logger = LoggerFactory.getLogger(BaseController.class);

    protected <T> ResponseEntity<APIResponse<T>> handleRequest(String authToken, LimitedOperation op, int opIncrementVal, Function<String, T> action) throws Exception {
        String userId = firebaseAuthService.authorizeUser(authToken);
        TierInfo tierInfo = limitsOrchestrator.getUserTierInfo(userId);
        limitsOrchestrator.verifyLimit(tierInfo, op, opIncrementVal);

        T result = action.apply(userId);

        tierInfo = limitsOrchestrator.updateLimit(tierInfo, op, opIncrementVal);

        PayloadMetadata meta = new PayloadMetadata(tierInfo);
        APIResponse<T> apiResponse = new APIResponse<T>(meta, result);

        return ResponseEntity.ok(apiResponse);
    }
}
