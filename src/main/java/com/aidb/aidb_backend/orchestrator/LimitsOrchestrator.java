package com.aidb.aidb_backend.orchestrator;

import com.aidb.aidb_backend.exception.UserLimitExceededException;
import com.aidb.aidb_backend.model.api.TierInfo;
import com.aidb.aidb_backend.model.firestore.Tier;
import com.aidb.aidb_backend.model.firestore.UserLimitsUsage;
import com.aidb.aidb_backend.model.firestore.util.LimitedOperation;
import com.aidb.aidb_backend.service.database.firestore.TierService;
import com.aidb.aidb_backend.service.database.firestore.UserLimitsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
public class LimitsOrchestrator {

    @Autowired
    TierService tierService;

    @Autowired
    UserLimitsService userLimitsService;

    private static final Logger logger = LoggerFactory.getLogger(LimitsOrchestrator.class);

    public TierInfo getUserTierInfo(String userId) throws Exception {
        UserLimitsUsage userLimitsUsage = userLimitsService.getUserLimitsById(userId);
        Tier tier = tierService.getTier(userLimitsUsage.getTier());

        return TierInfo.from(userLimitsUsage, tier);
    }

    public void verifyLimit(TierInfo tierInfo, LimitedOperation op, int opIncrementVal) {
        int curUsage = getUsageByOperation(op);
        int limit = getLimitByOperation(op);

        if (curUsage + opIncrementVal <= limit) {
            throw new UserLimitExceededException("Exceeded limit: " + op.name());
        }
    }


    public TierInfo updateLimit(TierInfo tierInfo, LimitedOperation operation, int opIncrementVal) {
        // Resume


        return tierInfo;
    }

    private int getUsageByOperation(LimitedOperation operation) {
        // TODO
        return -1;
    }

    private TierInfo incrementUsage(TierInfo tierInfo, LimitedOperation operation, int opIncreaseVal) {
        // TODO

        return tierInfo;
    }

    private int getLimitByOperation(LimitedOperation operation) {
        // TODO
        return -1;
    }

}
