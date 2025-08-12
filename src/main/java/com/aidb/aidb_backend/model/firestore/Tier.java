package com.aidb.aidb_backend.model.firestore;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Data;

@Data
public class Tier {
    @DocumentId
    private String id;

    private String tierName;

    private int queryLimit;

    private int translationLimit;

    private int dataRowUsage;

    private int projectLimit;

    private int tablesPerProjectLimit;
}
