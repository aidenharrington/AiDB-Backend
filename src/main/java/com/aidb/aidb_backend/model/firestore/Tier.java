package com.aidb.aidb_backend.model.firestore;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Data;

@Data
public class Tier {
    @DocumentId
    private String id;

    // Must match a TierId enum value
    private String name;

    private Long queryLimit;

    private Long translationLimit;

    private Long dataRowLimit;

    private Long projectLimit;

    private Long maxFileSize;

}
