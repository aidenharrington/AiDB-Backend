package com.aidb.aidb_backend.model.firestore.util;

import lombok.Getter;
import org.hibernate.metamodel.mapping.ordering.TranslationContext;

@Getter
public enum LimitedOperation {
    QUERY,
    TRANSLATION,
    DATA_ROW,
    PROJECT,
    TABLES;

    // TODO - Tier - Fix or remove

//    private final String firestoreField;
//
//    LimitedOperation(String firestoreField) {
//        this.firestoreField = firestoreField;
//    }


}
