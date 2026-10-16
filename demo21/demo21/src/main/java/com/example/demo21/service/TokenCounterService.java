
package com.example.demo21.service;

import org.springframework.stereotype.Service;

@Service
public class TokenCounterService {

    public int estimateTokens(String text) {

        if (text == null || text.isBlank()) {
            return 0;
        }

        // Rough estimate: approximately 2 tokens per word.
        // This is not exact and must not be used for billing.
        return Math.max(1, text.trim().split("\\s+").length * 2);
    }
}
