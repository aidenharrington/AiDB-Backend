package com.aidb.aidb_backend.model.firestore.util;

import lombok.Getter;

@Getter
public enum LimitedOperation {
    QUERY("queryLimit", "queryLimitUsage"),
    TRANSLATION("translationLimit", "translationLimitUsage"),
    DATA_ROW("dataRowLimit", "dataRowLimitUsage"),
    PROJECT("projectLimit", "projectLimitUsage");


    private final String limitFieldName;
    private final String usageFieldName;

    LimitedOperation(String limitFieldName, String usageFieldName) {
        this.limitFieldName = limitFieldName;
        this.usageFieldName = usageFieldName;
    }


}
