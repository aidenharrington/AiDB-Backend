package com.aidb.aidb_backend.model.firestore;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import lombok.Data;

@Data
public class Query {

    @DocumentId
    private String id;

    @PropertyName("user_id")
    private String userId;

    @PropertyName("nl_query")
    private String nlQuery;

    @PropertyName("sql_query")
    private String sqlQuery;

    private Timestamp timestamp;
}
