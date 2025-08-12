package com.aidb.aidb_backend.service.database.firestore;

import com.aidb.aidb_backend.model.firestore.Tier;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

@Service
public class TierService {

    @Autowired
    private Firestore firestore;

    @Setter
    private HashMap<String, Tier> tierMap;

    private static final Logger logger = LoggerFactory.getLogger(TierService.class);

    private static final String TIER_COLLECTION = "tier";

    public HashMap<String, Tier> getTierMap() throws Exception {

        if (tierMap != null && !tierMap.isEmpty()) {
            return this.tierMap;
        }

        CollectionReference tierCollection = firestore.collection(TIER_COLLECTION);
        ApiFuture<QuerySnapshot> querySnapshot = tierCollection.get();

        List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();

        tierMap = new HashMap<>();
        for (DocumentSnapshot doc : documents) {
            Tier tier = doc.toObject(Tier.class);
            if (tier != null && tier.getName() != null) {
                tierMap.put(tier.getName(), tier);
            }
        }

        return tierMap;
    }

}
