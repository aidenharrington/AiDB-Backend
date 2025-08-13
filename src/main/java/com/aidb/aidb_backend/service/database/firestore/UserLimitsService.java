package com.aidb.aidb_backend.service.database.firestore;

import com.aidb.aidb_backend.exception.UserNotFoundException;
import com.aidb.aidb_backend.model.api.TierInfo;
import com.aidb.aidb_backend.model.firestore.UserLimitsUsage;
import com.aidb.aidb_backend.model.firestore.util.LimitedOperation;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
public class UserLimitsService {

    @Autowired
    Firestore firestore;

    private final String USER_COLLECTION = "user_limits";

    private static final Logger logger = LoggerFactory.getLogger(UserLimitsService.class);


    public UserLimitsUsage getUserLimitsById(String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot snapshot = firestore.collection(USER_COLLECTION).document(id).get().get();

        if (snapshot.exists()) {
            return snapshot.toObject(UserLimitsUsage.class);
        } else {
            throw new UserNotFoundException("Could not find user limit by id");
        }
    }

    public String addUserLimits(UserLimitsUsage userLimitsUsage) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(USER_COLLECTION).document();
        Timestamp currentTime = Timestamp.now();

        userLimitsUsage.setCreatedAt(currentTime);
        userLimitsUsage.setLastUpdated(currentTime);

        ApiFuture<WriteResult> future = docRef.set(userLimitsUsage);
        future.get();
        return docRef.getId();
    }

    public Long updateLimitUsage(LimitedOperation operation, int opIncrementVal) {
        DocumentReference docRef = firestore.collection(USER_COLLECTION).document();

        try {
            return firestore.runTransaction((Transaction.Function<Long>) transaction -> {
                DocumentSnapshot snapshot = transaction.get(docRef).get();

                Long currentUsage = snapshot.getLong(operation.getUsageFieldName());
                Long updatedUsage = currentUsage == null ? (long) opIncrementVal : currentUsage + opIncrementVal;

                transaction.update(docRef, operation.getUsageFieldName(), updatedUsage);
                return updatedUsage;
            }).get();
        } catch (Exception e) {
            logger.error("Graceful error occurred while updating limit usage: {}", e.getMessage());

            // Exception handled gracefully
            return null;
        }
    }


}
