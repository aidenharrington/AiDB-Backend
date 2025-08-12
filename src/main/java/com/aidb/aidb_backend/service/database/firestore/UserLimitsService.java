package com.aidb.aidb_backend.service.database.firestore;

import com.aidb.aidb_backend.exception.UserNotFoundException;
import com.aidb.aidb_backend.model.firestore.UserLimits;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
public class UserLimitsService {

    @Autowired
    Firestore firestore;

    private final String USER_COLLECTION = "users";

    public UserLimits getUserLimitsById(String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot snapshot = firestore.collection(USER_COLLECTION).document(id).get().get();

        if (snapshot.exists()) {
            return snapshot.toObject(UserLimits.class);
        } else {
            throw new UserNotFoundException("Could not find user limit by id");
        }
    }

    public String addUserLimits(UserLimits userLimits) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(USER_COLLECTION).document();
        Timestamp currentTime = Timestamp.now();

        userLimits.setCreatedAt(currentTime);
        userLimits.setLastUpdated(currentTime);

        ApiFuture<WriteResult> future = docRef.set(userLimits);
        future.get();
        return docRef.getId();
    }

}
