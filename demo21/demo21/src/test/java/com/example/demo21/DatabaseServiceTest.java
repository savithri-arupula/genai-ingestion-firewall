
package com.example.demo21;

import com.example.demo21.service.DatabaseService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseServiceTest {

    private DatabaseService db;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Create a separate database for each test.
        db = new DatabaseService(tempDir.resolve("test.db"));
        db.initializeDatabase();
    }

    @Test
    void messagesAreSavedAndReturnedInOrder() {
        String sessionId = UUID.randomUUID().toString();

        db.addChatMessage(sessionId, "user", "Where is my order?");
        db.addChatMessage(
                sessionId,
                "assistant",
                "Let me explain how to track it."
        );

        List<Map<String, String>> history =
                db.getChatHistory(sessionId);

        assertEquals(2, history.size());

        assertEquals("user", history.get(0).get("role"));
        assertEquals(
                "Where is my order?",
                history.get(0).get("content")
        );

        assertEquals("assistant", history.get(1).get("role"));
        assertEquals(
                "Let me explain how to track it.",
                history.get(1).get("content")
        );

        List<Map<String, String>> messages =
                db.getConversationMessages(sessionId);

        assertEquals(2, messages.size());
        assertEquals("user", messages.get(0).get("role"));
        assertEquals("assistant", messages.get(1).get("role"));
    }

    @Test
    void historyIsLimitedToMostRecentMessages() {
        String sessionId = UUID.randomUUID().toString();

        for (int i = 0; i < DatabaseService.MAX_HISTORY_MESSAGES + 4; i++) {
            db.addChatMessage(sessionId, "user", "message " + i);
        }

        List<Map<String, String>> history =
                db.getChatHistory(sessionId);

        assertEquals(
                DatabaseService.MAX_HISTORY_MESSAGES,
                history.size()
        );

        assertEquals(
                "message " + (DatabaseService.MAX_HISTORY_MESSAGES + 3),
                history.get(history.size() - 1).get("content")
        );

        assertEquals(
                "message 4",
                history.get(0).get("content")
        );
    }

    @Test
    void conversationTitleComesFromFirstUserMessage() {
        String sessionId = UUID.randomUUID().toString();

        db.ensureConversation(sessionId);

        db.addChatMessage(
                sessionId,
                "user",
                "My payment was deducted twice"
        );

        db.addChatMessage(sessionId, "user", "Any update?");

        List<Map<String, String>> conversations =
                db.listConversations();

        assertEquals(1, conversations.size());

        Map<String, String> conversation = conversations.get(0);

        assertEquals(sessionId, conversation.get("session_id"));

        assertEquals(
                "My payment was deducted twice",
                conversation.get("title")
        );

        assertEquals("Any update?", conversation.get("preview"));
    }

    @Test
    void invalidRoleIsRejected() {
        String sessionId = UUID.randomUUID().toString();

        assertThrows(
                IllegalArgumentException.class,
                () -> db.addChatMessage(
                        sessionId,
                        "system",
                        "not allowed"
                )
        );
    }
}
