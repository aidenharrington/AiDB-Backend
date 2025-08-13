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

    private int queryLimit;

    private int queryLimitUsage;

    private int translationLimit;

    private int translationLimitUsage;

    private int dataRowLimit;

    private int dataRowLimitUsage;

    private int projectLimit;

    private int projectLimitUsage;

    private int maxFileSize;

    public static TierInfo from(UserLimitsUsage usage, Tier tier) {
        return TierInfo.builder()
                .name(tier.getTierName())
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
