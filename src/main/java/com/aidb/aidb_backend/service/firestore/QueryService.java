package com.aidb.aidb_backend.service.firestore;

import com.aidb.aidb_backend.config.FirebaseConfig;
import com.aidb.aidb_backend.model.firestore.Query;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
public class QueryService {

    @Autowired
    private Firestore firestore;

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    private static final String QUERY_COLLECTION  = "queries";

    public String addQuery(Query query) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(QUERY_COLLECTION).document();
        query.setTimestamp(Timestamp.now());
        ApiFuture<WriteResult> future = docRef.set(query);
        future.get();
        return docRef.getId();
    }

    public Query getQuery(String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot snapshot = firestore.collection(QUERY_COLLECTION).document(id).get().get();
        return snapshot.exists() ? snapshot.toObject(Query.class) : null;
    }
}
