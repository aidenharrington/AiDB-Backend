package com.aidb.aidb_backend.controller;

import com.aidb.aidb_backend.model.api.APIResponse;
import com.aidb.aidb_backend.model.dto.FeedbackDTO;
import com.aidb.aidb_backend.orchestrator.FeedbackOrchestrator;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/feedback")
public class FeedbackController extends BaseController {

    @Autowired
    FeedbackOrchestrator feedbackOrchestrator;

    @PostMapping
    public ResponseEntity<APIResponse<String>> submitFeedback(@RequestHeader("Authorization") String authToken,
                                                              @Valid @RequestBody FeedbackDTO feedback) throws Exception {
        return handleRequest(authToken,
                (user, args) -> feedbackOrchestrator.submitFeedback(user, feedback)
        );
    }
}
