package com.aidb.aidb_backend.service.database.firestore;

import com.aidb.aidb_backend.exception.UserNotFoundException;
import com.aidb.aidb_backend.exception.http.InternalServerErrorException;
import com.aidb.aidb_backend.model.firestore.UserLimitsUsage;
import com.aidb.aidb_backend.model.firestore.util.LimitedOperation;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class UserLimitsService {

    @Autowired
    public Firestore firestore;

    private final String USER_LIMITS_COLLECTION = "user_limits";

    private static final Logger logger = LoggerFactory.getLogger(UserLimitsService.class);


    public UserLimitsUsage getUserLimitsById(String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot snapshot = firestore.collection(USER_LIMITS_COLLECTION).document(id).get().get();

        if (snapshot.exists()) {
            return snapshot.toObject(UserLimitsUsage.class);
        } else {
            throw new UserNotFoundException("Could not find user limit by id");
        }
    }

    public String addUserLimits(UserLimitsUsage userLimitsUsage) throws ExecutionException, InterruptedException {
        if (userLimitsUsage.getId() == null || userLimitsUsage.getId().isEmpty()) {
            throw new IllegalArgumentException("User ID must be set before saving to Firestore");
        }

        DocumentReference docRef = firestore.collection(USER_LIMITS_COLLECTION)
                .document(userLimitsUsage.getId());

        // Check if document already exists
        DocumentSnapshot snapshot = docRef.get().get();
        if (snapshot.exists()) {
            throw new InternalServerErrorException("User limits already exist: " + userLimitsUsage.getId());
        }

        // Set timestamps
        Timestamp currentTime = Timestamp.now();
        userLimitsUsage.setCreatedAt(currentTime);
        userLimitsUsage.setLastUpdated(currentTime);

        // Write to Firestore
        ApiFuture<WriteResult> future = docRef.set(userLimitsUsage);
        future.get();

        return docRef.getId();
    }

    public Long updateLimitUsage(String userId, LimitedOperation operation, int opIncrementVal) {
        DocumentReference docRef = firestore.collection(USER_LIMITS_COLLECTION).document(userId);

        try {
            return firestore.runTransaction((Transaction.Function<Long>) transaction -> {
                DocumentSnapshot snapshot = transaction.get(docRef).get();

                Long curUsage = snapshot.getLong(operation.getUsageFieldName());
                Long updatedUsage = curUsage == null ? (long) opIncrementVal : curUsage + opIncrementVal;

                transaction.update(docRef,
                        operation.getUsageFieldName(), updatedUsage,
                        "lastUpdated", FieldValue.serverTimestamp()
                );

                return updatedUsage;
            }).get();
        } catch (Exception e) {
            logger.error("Graceful error occurred while updating limit usage: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Resets usage fields for all users and updates lastUpdated timestamp.
     * Data limit is not reset
     * This method should only be called from scheduled reset.
     */
    public void resetUsageForAllUsers(Timestamp now) throws Exception {
        ApiFuture<QuerySnapshot> query = firestore.collection(USER_LIMITS_COLLECTION).get();
        List<QueryDocumentSnapshot> documents = query.get().getDocuments();

        for (QueryDocumentSnapshot doc : documents) {
            UserLimitsUsage usage = doc.toObject(UserLimitsUsage.class);
            usage.setQueryLimitUsage(0L);
            usage.setTranslationLimitUsage(0L);
            usage.setProjectLimitUsage(0L);
            usage.setLastUpdated(now);

            firestore.collection(USER_LIMITS_COLLECTION)
                    .document(usage.getId())
                    .set(usage);
        }
    }

}
