
package com.example.demo21.controller;

import com.example.demo21.dto.IngestionRequest;
import com.example.demo21.dto.IngestionResult;
import com.example.demo21.service.LlmService;
import com.example.demo21.service.TokenCounterService;


import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class IngestionController {

    private final LlmService llmService;
    private final TokenCounterService tokenCounterService;

    public IngestionController(
            LlmService llmService,
            TokenCounterService tokenCounterService) {
        this.llmService = llmService;
        this.tokenCounterService = tokenCounterService;
    }

    @GetMapping("/health")
    public Map<String, String> healthCheck() {
        return Map.of(
                "status", "ok",
                "message", "Ingestion Firewall API is running"
        );
    }

    @PostMapping("/api/analyze")
    public IngestionResult analyze(
            @Valid @RequestBody IngestionRequest request) {

        String message = request.getMessage().trim();

        if (message.isEmpty()) {
            throw new IllegalArgumentException(
                    "Message cannot be empty.");
        }

        IngestionResult result = llmService.analyzeMessage(message);

        result.getToken_metadata().setEstimated_input_tokens(
                tokenCounterService.estimateTokens(message));

        return result;
    }
}
