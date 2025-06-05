package com.aidb.aidb_backend.model.firestore;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Data;

@Data
public class Query {

    @DocumentId
    private String id;

    private String userId;

    private String nlQuery;

    private String sqlQuery;

    private Timestamp timestamp;
}
