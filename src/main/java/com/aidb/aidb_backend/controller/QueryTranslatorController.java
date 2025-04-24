package com.aidb.aidb_backend.controller;

import com.aidb.aidb_backend.config.FirebaseConfig;
import com.aidb.aidb_backend.model.firestore.Query;
import com.aidb.aidb_backend.security.authorization.FirebaseAuthService;
import com.aidb.aidb_backend.service.api.QueryTranslatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/query-translator")
public class QueryTranslatorController {

    @Autowired
    private QueryTranslatorService queryTranslatorService;

    @Autowired
    private FirebaseAuthService firebaseAuthService;

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @PostMapping("/generate-sql")
    public String generateSql(@RequestBody String nlQuery) {
        String userId = firebaseAuthService.getUserIdPlaceholder();
        Query query = queryTranslatorService.translateToSql(userId, nlQuery);

        return query.getSqlQuery();
    }

}
