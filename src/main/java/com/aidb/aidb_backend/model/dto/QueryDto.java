package com.aidb.aidb_backend.model.dto;

import com.aidb.aidb_backend.model.firestore.Query;
import lombok.Data;

import com.google.cloud.Timestamp;

@Data
public class QueryDto {
    private String nlQuery;
    private String sqlQuery;
    private String timestamp;

    public QueryDto(String nlQuery, String sqlQuery, Timestamp timestamp) {
        this.nlQuery = nlQuery;
        this.sqlQuery = sqlQuery;
        this.timestamp = formatTimestamp(timestamp);
    }

    public QueryDto(Query query) {
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
