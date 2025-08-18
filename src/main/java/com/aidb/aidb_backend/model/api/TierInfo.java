package com.aidb.aidb_backend.model.api;

import com.aidb.aidb_backend.model.firestore.Tier;
import com.aidb.aidb_backend.model.firestore.UserLimitsUsage;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TierInfo {

    private String name;

    private String userId;

    private Long queryLimit;

    private Long queryLimitUsage;

    private Long translationLimit;

    private Long translationLimitUsage;

    private Long dataRowLimit;

    private Long dataRowLimitUsage;

    private Long projectLimit;

    private Long projectLimitUsage;

    private Long maxFileSize;

    public static TierInfo from(UserLimitsUsage usage, Tier tier) {
        return TierInfo.builder()
                .name(tier.getName())
                .userId(usage.getId())
                .queryLimit(tier.getQueryLimit())
                .queryLimitUsage(usage.getQueryLimitUsage())
                .translationLimit(tier.getTranslationLimit())
                .translationLimitUsage(usage.getTranslationLimitUsage())
                .dataRowLimit(tier.getDataRowLimit())
                .dataRowLimitUsage(usage.getDataRowLimitUsage())
                .projectLimit(tier.getProjectLimit())
                .projectLimitUsage(usage.getProjectLimitUsage())
                .maxFileSize(tier.getMaxFileSize())
                .build();
    }
}
