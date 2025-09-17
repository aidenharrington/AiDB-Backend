package com.aidb.aidb_backend.controller;

import com.aidb.aidb_backend.model.api.PayloadMetadata;
import com.aidb.aidb_backend.model.api.TierInfo;
import com.aidb.aidb_backend.orchestrator.LimitsOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.aidb.aidb_backend.model.firestore.User;

@RestController
@RequestMapping("/metadata")
public class MetadataController extends BaseController {

    @Autowired
    LimitsOrchestrator limitsOrchestrator;

    @GetMapping("/tier")
    public ResponseEntity<TierInfo> getTierInfo(@RequestHeader("Authorization") String authToken) throws Exception {
        return handleMetadataRequest(authToken,
                (user, args) -> limitsOrchestrator.getUserTierInfo(user.getUserId()));
    }
}
