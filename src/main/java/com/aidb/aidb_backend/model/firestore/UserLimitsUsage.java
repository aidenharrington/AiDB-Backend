package com.aidb.aidb_backend.model.firestore;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Data;

@Data
public class UserLimitsUsage {

    // Corresponds to Firebase Authentication User UID
    @DocumentId
    private String id;

    private String tier;

    private Long queryLimitUsage;

    private Long translationLimitUsage;

    private Long dataRowLimitUsage;

    private Long projectLimitUsage;

    private Timestamp createdAt;

    private Timestamp lastUpdated;




}
