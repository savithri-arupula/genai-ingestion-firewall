
package com.example.demo21.dto;

public class TokenMetadata {

    private int estimated_input_tokens = 0;

    private String note =
        "Estimated locally; may differ from provider token counts.";

    public TokenMetadata() {
    }

    public int getEstimated_input_tokens() {
        return estimated_input_tokens;
    }

    public void setEstimated_input_tokens(int estimated_input_tokens) {
        this.estimated_input_tokens = estimated_input_tokens;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
