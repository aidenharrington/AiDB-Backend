package com.aidb.aidb_backend.model.firestore;

import com.aidb.aidb_backend.model.firestore.util.TierId;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Data;

@Data
public class UserLimitsUsage {

    // Corresponds to Firebase Authentication User UID
    @DocumentId
    private String id;

    private TierId tierId;

    private Long queryLimitUsage;

    private Long translationLimitUsage;

    private Long dataRowLimitUsage;

    private Long projectLimitUsage;

    private Timestamp createdAt;

    private Timestamp lastUpdated;

    public UserLimitsUsage() {};


   private UserLimitsUsage(String id) {
       this.id = id;
       this.tierId = TierId.FREE;

       this.queryLimitUsage = 0L;
       this.translationLimitUsage = 0L;
       this.dataRowLimitUsage = 0L;
       this.projectLimitUsage = 0L;

       // Timestamps set by service
       this.createdAt = null;
       this.lastUpdated = null;
   }

   public static UserLimitsUsage createNewUserLimits(String id) {
       return new UserLimitsUsage(id);
   }


}
