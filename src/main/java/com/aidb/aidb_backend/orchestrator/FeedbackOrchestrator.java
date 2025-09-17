package com.aidb.aidb_backend.orchestrator;

import com.aidb.aidb_backend.model.dto.FeedbackDTO;
import com.aidb.aidb_backend.model.firestore.User;
import com.aidb.aidb_backend.service.api.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FeedbackOrchestrator {

    @Autowired
    EmailService emailService;

    public String submitFeedback(User user, FeedbackDTO feedback) {
        emailService.sendSupportEmail(user, feedback);
        emailService.sendConfirmationEmail(user, feedback);

        return "Success";
    }
}
