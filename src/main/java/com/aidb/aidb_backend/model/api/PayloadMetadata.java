package com.aidb.aidb_backend.model.api;

import lombok.Data;

@Data
public class PayloadMetadata {
    private TierInfo tier;
    
    public PayloadMetadata(TierInfo tier) {
        this.tier = tier;
    }
}
