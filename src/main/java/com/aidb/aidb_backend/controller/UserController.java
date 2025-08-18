package com.aidb.aidb_backend.controller;

import com.aidb.aidb_backend.model.api.APIResponse;
import com.aidb.aidb_backend.model.dto.UserDTO;
import com.aidb.aidb_backend.orchestrator.UserOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController extends BaseController {

    @Autowired
    UserOrchestrator userOrchestrator;


    @PostMapping
    public ResponseEntity<APIResponse<UserDTO>> setupNewUser(@RequestHeader("Authorization") String authToken) throws Exception {
        return handleRequest(authToken,
                null,
                -1,
                (userId, args) -> userOrchestrator.setupNewUser(userId)
        );
    }
}
