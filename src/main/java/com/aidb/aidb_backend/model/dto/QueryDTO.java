package com.aidb.aidb_backend.model.dto;

import com.aidb.aidb_backend.model.firestore.Query;
import lombok.Data;

import com.google.cloud.Timestamp;

@Data
public class QueryDTO {
    private String id;
    private String nlQuery;
    private String sqlQuery;
    private String timestamp;

    public QueryDTO(String id, String nlQuery, String sqlQuery, Timestamp timestamp) {
        this.id = id;
        this.nlQuery = nlQuery;
        this.sqlQuery = sqlQuery;
        this.timestamp = formatTimestamp(timestamp);
    }

    public QueryDTO(Query query) {
        this.id = query.getId() != null ? String.valueOf(query.getId()) : null;
        this.nlQuery = query.getNlQuery();
        this.sqlQuery = query.getSqlQuery();
        this.timestamp = formatTimestamp(query.getTimestamp());
    }

    private String formatTimestamp(Timestamp timestamp) {
        return timestamp
                .toDate()
                .toInstant()
                .toString();
    }
}
