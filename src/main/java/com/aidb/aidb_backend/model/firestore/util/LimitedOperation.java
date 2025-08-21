package com.aidb.aidb_backend.model.firestore.util;
import com.aidb.aidb_backend.model.api.TierInfo;

import lombok.Getter;

import java.util.function.BiConsumer;
import java.util.function.Function;

@Getter
public enum LimitedOperation {
    QUERY("queryLimit", "queryLimitUsage",
            TierInfo::getQueryLimit,
            TierInfo::getQueryLimitUsage,
            TierInfo::setQueryLimitUsage),

    TRANSLATION("translationLimit", "translationLimitUsage",
            TierInfo::getTranslationLimit,
            TierInfo::getTranslationLimitUsage,
            TierInfo::setTranslationLimitUsage),

    DATA_ROW("dataRowLimit", "dataRowLimitUsage",
            TierInfo::getDataRowLimit,
            TierInfo::getDataRowLimitUsage,
            TierInfo::setDataRowLimitUsage),

    PROJECT("projectLimit", "projectLimitUsage",
            TierInfo::getProjectLimit,
            TierInfo::getProjectLimitUsage,
            TierInfo::setProjectLimitUsage);

    private final String limitFieldName;
    private final String usageFieldName;
    private final Function<TierInfo, Long> limitGetter;
    private final Function<TierInfo, Long> usageGetter;
    private final BiConsumer<TierInfo, Long> usageSetter;

    LimitedOperation(String limitFieldName, String usageFieldName,
                     Function<TierInfo, Long> limitGetter,
                     Function<TierInfo, Long> usageGetter,
                     BiConsumer<TierInfo, Long> usageSetter) {
        this.limitFieldName = limitFieldName;
        this.usageFieldName = usageFieldName;
        this.limitGetter = limitGetter;
        this.usageGetter = usageGetter;
        this.usageSetter = usageSetter;
    }

    public Long getLimit(TierInfo tierInfo) {
        return limitGetter.apply(tierInfo);
    }

    public Long getUsage(TierInfo tierInfo) {
        return usageGetter.apply(tierInfo);
    }

    public void setUsage(TierInfo tierInfo, Long value) {
        usageSetter.accept(tierInfo, value);
    }
}

