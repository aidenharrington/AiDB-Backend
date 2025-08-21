package com.aidb.aidb_backend.model.api;

import lombok.Data;

@Data
public class APIResponse<T> {
    private PayloadMetadata meta;
    private T data;

    public APIResponse(PayloadMetadata meta, T data) {
        this.meta = meta;
        this.data = data;
    }
    
}

