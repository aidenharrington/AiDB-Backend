package com.aidb.aidb_backend.orchestrator;

import com.aidb.aidb_backend.model.firestore.UserLimitUsage;
import com.aidb.aidb_backend.model.firestore.util.LimitedOperation;
import com.aidb.aidb_backend.service.database.firestore.TierService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LimitsOrchestrator {

    @Autowired
    TierService tierService;

    private static final Logger logger = LoggerFactory.getLogger(LimitsOrchestrator.class);

    public boolean verifyLimit(UserLimitUsage userLimitUsage, LimitedOperation operation) {
        return false;
    }


    public UserLimitUsage updateLimit(UserLimitUsage userLimitUsage, LimitedOperation operation) {
        // Resume

        return userLimitUsage;
    }

}
