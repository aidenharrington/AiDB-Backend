package com.aidb.aidb_backend.model.firestore;

import com.aidb.aidb_backend.model.dto.QueryDTO;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Data;

@Data
public class Query {

    @DocumentId
    private String id;

    private String userId;

    private String projectId;

    private String nlQuery;

    private String sqlQuery;

    private Status status;

    private Timestamp timestamp;

    public Query() {}

    public Query(QueryDTO dto) {
        this.id = dto.getId();
        this.userId = dto.getUserId();
        this.projectId = dto.getProjectId() != null ? dto.getProjectId() : null;
        this.nlQuery = dto.getNlQuery();
        this.sqlQuery = dto.getSqlQuery();
        this.status = dto.getStatus();
        this.timestamp = dto.getTimestamp() != null ? Timestamp.parseTimestamp(dto.getTimestamp()) : null;
    }
}
