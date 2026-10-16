
package com.example.demo21.dto;

public class IngestionResult {

    private String intent;
    private String risk_profile;
    private String priority;
    private boolean requires_action;
    private String summary;
    private TokenMetadata token_metadata = new TokenMetadata();

    public IngestionResult() {
    }

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }

    public String getRisk_profile() { return risk_profile; }
    public void setRisk_profile(String risk_profile) {
        this.risk_profile = risk_profile;
    }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public boolean isRequires_action() { return requires_action; }
    public void setRequires_action(boolean requires_action) {
        this.requires_action = requires_action;
    }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public TokenMetadata getToken_metadata() { return token_metadata; }
    public void setToken_metadata(TokenMetadata token_metadata) {
        this.token_metadata = token_metadata;
    }
}
