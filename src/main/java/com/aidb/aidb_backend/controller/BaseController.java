package com.aidb.aidb_backend.controller;

import com.aidb.aidb_backend.model.api.APIResponse;
import com.aidb.aidb_backend.model.api.ActionWithUserId;
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

    protected <T> ResponseEntity<APIResponse<T>> handleRequest(
            String authToken,
            LimitedOperation op,
            int opIncrementVal,
            ActionWithUserId<T> action,
            Object... args) throws Exception {

        // 1. Authorize user
        String userId = firebaseAuthService.authorizeUser(authToken);

        // 2. Get user tier info and verify limits
        TierInfo tierInfo = null;
        if (op != null) {
            tierInfo = limitsOrchestrator.getUserTierInfo(userId);
            limitsOrchestrator.verifyLimit(tierInfo, op, opIncrementVal);
        }


        // 3. Execute the action with userId + extra args
        T result = action.apply(userId, args);

        // 4. Update limit usage
        if (op != null && tierInfo != null) {
            tierInfo = limitsOrchestrator.updateLimit(tierInfo, op, opIncrementVal);
        }

        // 5. Build response
        PayloadMetadata meta = new PayloadMetadata(tierInfo);
        APIResponse<T> apiResponse = new APIResponse<>(meta, result);

        return ResponseEntity.ok(apiResponse);
    }

}
