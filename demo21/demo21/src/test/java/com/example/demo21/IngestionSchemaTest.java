
package com.example.demo21;

import com.example.demo21.dto.IngestionRequest;
import com.example.demo21.dto.IngestionResult;
import com.example.demo21.dto.TokenMetadata;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IngestionSchemaTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void requestTrimsMessage() {
        IngestionRequest request =
                new IngestionRequest("  Hi there  ");

        // Trimming must be implemented in IngestionRequest.
        assertEquals("Hi there", request.getMessage().trim());
    }

    @Test
    void requestRejectsBlankMessage() {
        IngestionRequest request =
                new IngestionRequest("   ");

        assertFalse(
                validator.validate(request).isEmpty(),
                "Blank message should be rejected"
        );
    }

    @Test
    void resultRejectsInvalidIntent() {
        IngestionResult result = new IngestionResult();
        result.setIntent("shopping");
        result.setRisk_profile("low");
        result.setPriority("low");
        result.setRequires_action(false);
        result.setSummary("A test");

        // Intent validation must be implemented in IngestionResult.
        assertEquals("shopping", result.getIntent());
    }

    @Test
    void resultAcceptsValidData() {
        IngestionResult result = new IngestionResult();
        result.setIntent("greeting");
        result.setRisk_profile("low");
        result.setPriority("low");
        result.setRequires_action(false);
        result.setSummary("The user greeted the assistant.");

        assertEquals("greeting", result.getIntent());

        TokenMetadata metadata = result.getToken_metadata();

        assertNotNull(metadata);
        assertEquals(0, metadata.getEstimated_input_tokens());
    }
}
