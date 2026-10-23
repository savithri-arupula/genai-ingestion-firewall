
package com.example.demo21.controller;

import com.example.demo21.dto.IngestionRequest;
import com.example.demo21.dto.IngestionResult;
import com.example.demo21.service.LlmService;
import com.example.demo21.service.ResponseService;
import com.example.demo21.service.TokenCounterService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
public class IngestionController {

    private final LlmService llmService;
    private final ResponseService responseService;
    private final TokenCounterService tokenCounterService;

    public IngestionController(
            LlmService llmService,
            ResponseService responseService,
            TokenCounterService tokenCounterService) {

        this.llmService = llmService;
        this.responseService = responseService;
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

        String message = request.getMessage() == null
                ? ""
                : request.getMessage().trim();

        if (message.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Message cannot be empty."
            );
        }

        try {
            // Step 1: Classify the message
            IngestionResult result =
                    llmService.analyzeMessage(message);

            // Step 2: Estimate input tokens
            result.getToken_metadata().setEstimated_input_tokens(
                tokenCounterService.estimateTokens(message)
            );

            // Step 3: Generate the Markdown answer
            String responseMarkdown =
                    responseService.generateResponse(result);

            // Step 4: Add the answer to the result
            result.setResponse_markdown(responseMarkdown);

            // Step 5: Return the complete response
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
}
