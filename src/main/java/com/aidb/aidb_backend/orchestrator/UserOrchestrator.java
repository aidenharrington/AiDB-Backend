package com.aidb.aidb_backend.orchestrator;

import com.aidb.aidb_backend.model.dto.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserOrchestrator {

    @Autowired
    LimitsOrchestrator limitsOrchestrator;

    public UserDTO setupNewUser(String userId) throws Exception {
        limitsOrchestrator.setupLimitsForNewUser(userId);

        return new UserDTO(userId);
    }
}
