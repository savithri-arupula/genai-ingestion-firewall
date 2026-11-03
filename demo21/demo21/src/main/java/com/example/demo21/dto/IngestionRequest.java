
package com.example.demo21.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class IngestionRequest {

    @Size(min = 1, max = 100,
          message = "Session ID must be between 1 and 100 characters.")
    private String session_id;

    @NotBlank(message = "Message cannot contain only whitespace.")
    @Size(max = 5000,
          message = "Message must not exceed 5000 characters.")
    private String message;

    @JsonIgnore
    private boolean unknownFieldFound = false;

    public IngestionRequest() {
    }

    public IngestionRequest(String message) {
        setMessage(message);
    }

    public String getSession_id() {
        return session_id;
    }

    public void setSession_id(String session_id) {
        this.session_id = session_id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message == null ? null : message.trim();
    }

    @JsonAnySetter
    public void handleUnknownField(String name, Object value) {
        unknownFieldFound = true;
    }

    @JsonIgnore
    public boolean isUnknownFieldFound() {
        return unknownFieldFound;
    }
}
