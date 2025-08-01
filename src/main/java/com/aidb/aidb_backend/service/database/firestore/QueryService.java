package com.aidb.aidb_backend.service.database.firestore;

import com.aidb.aidb_backend.model.dto.QueryDto;
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

import static com.google.cloud.firestore.Query.Direction.DESCENDING;

@Service
public class QueryService {

    @Autowired
    private Firestore firestore;

    private static final Logger logger = LoggerFactory.getLogger(QueryService.class);

    private static final String QUERY_COLLECTION  = "queries";
    private static final String USER_ID = "userId";
    private static final String TIMESTAMP = "timestamp";

    public String addQuery(Query query) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(QUERY_COLLECTION).document();
        query.setTimestamp(Timestamp.now());
        ApiFuture<WriteResult> future = docRef.set(query);
        future.get();
        return docRef.getId();
    }

    public String addOrUpdateQuery(Query query) throws ExecutionException, InterruptedException {
        Long queryId = query.getId();

        if (queryId != null) {
            DocumentReference docRef = firestore.collection(QUERY_COLLECTION).document(queryId.toString());
            ApiFuture<DocumentSnapshot> docSnapFuture = docRef.get();
            DocumentSnapshot docSnap = docSnapFuture.get();

            if (docSnap.exists()) {
                ApiFuture<WriteResult> writeResult = docRef.set(query);
                writeResult.get();
                return queryId.toString();
            }
        }

        // Create new document
        query.setTimestamp(Timestamp.now());
        DocumentReference newDocRef = firestore.collection(QUERY_COLLECTION).document();
        ApiFuture<WriteResult> writeResult = newDocRef.set(query);
        writeResult.get();
        return newDocRef.getId();
    }

    public Query getQueryById(String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot snapshot = firestore.collection(QUERY_COLLECTION).document(id).get().get();
        return snapshot.exists() ? snapshot.toObject(Query.class) : null;
    }

    public List<QueryDto> getAllQueryDtos(String userId) throws ExecutionException, InterruptedException {
        CollectionReference queriesRef = firestore.collection(QUERY_COLLECTION);
        ApiFuture<QuerySnapshot> future = queriesRef
                .whereEqualTo(USER_ID, userId)
                .orderBy(TIMESTAMP, DESCENDING)
                .get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        List<QueryDto> results = new ArrayList<>();

        for (QueryDocumentSnapshot doc : documents) {
            Query query = doc.toObject(Query.class);
            QueryDto queryDto = new QueryDto(query);
            results.add(queryDto);
        }

        return results;
    }
}
