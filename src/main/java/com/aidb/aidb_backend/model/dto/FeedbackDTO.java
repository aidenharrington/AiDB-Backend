package com.aidb.aidb_backend.model.dto;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Data
public class FeedbackDTO {

    @NotBlank(message = "Type of feedback is required.")
    private String type;

    @NotBlank(message = "Message is required.")
    private String message;
}
