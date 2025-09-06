package com.aidb.aidb_backend.service.api;

import com.aidb.aidb_backend.model.dto.FeedbackDTO;
import com.aidb.aidb_backend.model.firestore.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${support.email}")
    private String supportEmail;

    @Value("${spring.mail.username}")
    private String senderEmail;

    private static final String CONFIRMATION_SUBJECT = "Thank you for your feedback";
    private static final String CONFIRMATION_BODY = "Hi,\n\n" +
            "I have received your %s report and I will review it shortly.\n" +
            "Thank you for helping improve AiDB.\n\n" +
            "Best,\n" +
            "Aiden";

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendSupportEmail(User user, FeedbackDTO feedback) {
        String subject = ("New " + feedback.getType() + " report from " + user.getEmail());
        sendEmail(senderEmail, supportEmail, subject, feedback.getMessage());
    }

    public void sendConfirmationEmail(User user, FeedbackDTO feedback) {
        String text = String.format(CONFIRMATION_BODY, feedback.getType());
        sendEmail(senderEmail, user.getEmail(), CONFIRMATION_SUBJECT, text);
    }

    public void sendEmail(String senderEmail, String recipientEmail, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(recipientEmail);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
