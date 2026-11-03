
package com.example.demo21.controller;

import com.example.demo21.dto.IngestionRequest;
import com.example.demo21.dto.IngestionResult;
import com.example.demo21.service.DatabaseService;
import com.example.demo21.service.LlmService;
import com.example.demo21.service.ResponseService;
import com.example.demo21.service.TokenCounterService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class IngestionController {

    private final LlmService llmService;
    private final ResponseService responseService;
    private final TokenCounterService tokenCounterService;
    private final DatabaseService databaseService;

    public IngestionController(
            LlmService llmService,
            ResponseService responseService,
            TokenCounterService tokenCounterService,
            DatabaseService databaseService) {

        this.llmService = llmService;
        this.responseService = responseService;
        this.tokenCounterService = tokenCounterService;
        this.databaseService = databaseService;
    }

    // Health check
    @GetMapping("/health")
    public Map<String, String> healthCheck() {
        return Map.of(
                "status", "ok",
                "message", "Ingestion Firewall API is running",
                "database", "sqlite"
        );
    }

    // List saved conversations
    @GetMapping("/api/conversations")
    public Map<String, Object> conversations() {
        return Map.of(
                "items", databaseService.listConversations()
        );
    }

    // Get messages from a conversation
    @GetMapping("/api/conversations/{sessionId}/messages")
    public Map<String, Object> conversationMessages(
            @PathVariable String sessionId) {

        validateSessionId(sessionId);

        return Map.of(
                "session_id", sessionId,
                "items", databaseService.getConversationMessages(sessionId)
        );
    }

    // Analyze a message and save the conversation
    @PostMapping("/api/analyze")
    public IngestionResult analyze(
            @Valid @RequestBody IngestionRequest request) {

        if (request.isUnknownFieldFound()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Unexpected field in request."
            );
        }

        String message = request.getMessage() == null
                ? ""
                : request.getMessage().trim();

        if (message.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Message cannot be empty."
            );
        }

        // Use the supplied session ID or create a new one.
        String sessionId = request.getSession_id();

        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        } else {
            validateSessionId(sessionId);
        }

        try {
            // Create the conversation if it does not exist.
            databaseService.ensureConversation(sessionId);

            // Classify the current message.
            IngestionResult result =
                    llmService.analyzeMessage(message);

            // Estimate input tokens.
            result.getToken_metadata()
                    .setEstimated_input_tokens(
                            tokenCounterService.estimateTokens(message)
                    );

            // Retrieve previous messages before saving the current one.
            List<Map<String, String>> history =
                    databaseService.getChatHistory(sessionId);

            // Generate a contextual response.
            String responseMarkdown =
                    responseService.generateResponse(
                            result,
                            history,
                            message
                    );

            // Save the user and assistant messages.
            databaseService.addChatMessage(
                    sessionId,
                    "user",
                    message
            );

            databaseService.addChatMessage(
                    sessionId,
                    "assistant",
                    responseMarkdown
            );

            // Return the response with session information.
            result.setSession_id(sessionId);
            result.setResponse_markdown(responseMarkdown);

            return result;

        } catch (ResponseStatusException ex) {
            throw ex;

        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "LLM request failed. Check the API key, model name, "
                            + "network, and provider limits.",
                    ex
            );
        }
    }

    // Validate conversation UUID
    private void validateSessionId(String sessionId) {
        try {
            UUID.fromString(sessionId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid conversation ID."
            );
        }
    }
}
