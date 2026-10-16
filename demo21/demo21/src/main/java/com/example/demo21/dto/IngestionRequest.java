
package com.example.demo21.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class IngestionRequest {

    @NotBlank(message = "Message cannot contain only whitespace.")
    @Size(max = 5000, message = "Message must not exceed 5000 characters.")
    private String message;

    public IngestionRequest() {
    }

    public IngestionRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
