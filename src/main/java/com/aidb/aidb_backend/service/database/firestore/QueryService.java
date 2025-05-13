package com.aidb.aidb_backend.service.database.firestore;

import com.aidb.aidb_backend.model.firestore.Query;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class QueryService {

    @Autowired
    private Firestore firestore;

    private static final Logger logger = LoggerFactory.getLogger(QueryService.class);

    private static final String QUERY_COLLECTION  = "queries";
    private static final String USER_ID = "user_id";

    public String addQuery(Query query) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(QUERY_COLLECTION).document();
        query.setTimestamp(Timestamp.now());
        ApiFuture<WriteResult> future = docRef.set(query);
        future.get();
        return docRef.getId();
    }

    public Query getQueryById(String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot snapshot = firestore.collection(QUERY_COLLECTION).document(id).get().get();
        return snapshot.exists() ? snapshot.toObject(Query.class) : null;
    }

    public List<Query> getAllQueries(String userId) throws ExecutionException, InterruptedException {
        CollectionReference queriesRef = firestore.collection(QUERY_COLLECTION);
        ApiFuture<QuerySnapshot> future = queriesRef.whereEqualTo(USER_ID, userId).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        List<Query> results = new ArrayList<>();

        for (QueryDocumentSnapshot doc : documents) {
            Query query = doc.toObject(Query.class);
            query.setId(doc.getId());
            results.add(query);
        }

        return results;


    }
}
