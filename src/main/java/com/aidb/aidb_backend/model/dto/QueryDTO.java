package com.aidb.aidb_backend.model.dto;

import com.aidb.aidb_backend.model.firestore.Query;
import com.aidb.aidb_backend.model.firestore.Status;
import lombok.Data;

import com.google.cloud.Timestamp;

@Data
public class QueryDTO {
    private String id;
    private String userId;
    private String projectId;
    private String nlQuery;
    private String sqlQuery;
    private Status status;
    private String timestamp;

    public QueryDTO() {};

    public QueryDTO(Query query) {
        this.id = query.getId();
        this.userId = query.getUserId();
        this.projectId = query.getProjectId() != null ? String.valueOf(query.getProjectId()) : null;
        this.nlQuery = query.getNlQuery();
        this.sqlQuery = query.getSqlQuery();
        this.status = query.getStatus();
        this.timestamp = formatTimestamp(query.getTimestamp());
    }

    private String formatTimestamp(Timestamp timestamp) {
        return timestamp
                .toDate()
                .toInstant()
                .toString();
    }
}
