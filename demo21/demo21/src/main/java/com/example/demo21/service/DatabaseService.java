
package com.example.demo21.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DatabaseService {

    public static final int MAX_HISTORY_MESSAGES = 20;

    private final String jdbcUrl;

    public DatabaseService() {
        this(System.getProperty(
                "sqlite.path",
                "backend/data/chat.db"
        ));
    }

    // Constructor used by tests with a separate database file.
    public DatabaseService(Path databasePath) {
        this(databasePath.toAbsolutePath().toString());
    }

    private DatabaseService(String databasePath) {
        this.jdbcUrl = "jdbc:sqlite:" + databasePath;
    }

    private Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }

        return connection;
    }

    // Automatically initialize the database when Spring starts.
    @PostConstruct
    public void initializeOnStartup() {
        initializeDatabase();
        System.out.println(
                "SQLite database initialized successfully!"
        );
        System.out.println("Database URL: " + jdbcUrl);
    }

    public void initializeDatabase() {
        try {
            String databasePath =
                    jdbcUrl.substring("jdbc:sqlite:".length());

            Path parent = Paths.get(databasePath)
                    .toAbsolutePath()
                    .getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (Connection connection = getConnection();
                 Statement statement = connection.createStatement()) {

                statement.execute("""
                    CREATE TABLE IF NOT EXISTS conversations (
                        id TEXT PRIMARY KEY,
                        title TEXT NOT NULL DEFAULT 'New Chat',
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL
                    )
                    """);

                statement.execute("""
                    CREATE TABLE IF NOT EXISTS messages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        conversation_id TEXT NOT NULL
                            REFERENCES conversations(id) ON DELETE CASCADE,
                        role TEXT NOT NULL
                            CHECK (role IN ('user', 'assistant')),
                        content TEXT NOT NULL,
                        created_at TEXT NOT NULL
                    )
                    """);

                statement.execute("""
                    CREATE INDEX IF NOT EXISTS
                    idx_messages_conversation_created
                    ON messages(conversation_id, created_at)
                    """);
            }

        } catch (SQLException | IOException e) {
            throw new IllegalStateException(
                    "Failed to initialize SQLite database", e
            );
        }
    }

    public void ensureConversation(String sessionId) {
        String sql = """
            INSERT OR IGNORE INTO conversations
                (id, title, created_at, updated_at)
            VALUES (?, 'New Chat', ?, ?)
            """;

        String now = Instant.now().toString();

        try (Connection connection = getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, sessionId);
            statement.setString(2, now);
            statement.setString(3, now);
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to create conversation", e
            );
        }
    }

    public void addChatMessage(
            String sessionId,
            String role,
            String content) {

        if (!"user".equals(role) && !"assistant".equals(role)) {
            throw new IllegalArgumentException(
                    "Invalid message role: " + role
            );
        }

        if (content == null) {
            throw new IllegalArgumentException(
                    "Message content cannot be null"
            );
        }

        String now = Instant.now().toString();

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try {
                // Create conversation if it does not exist.
                String conversationSql = """
                    INSERT OR IGNORE INTO conversations
                        (id, title, created_at, updated_at)
                    VALUES (?, 'New Chat', ?, ?)
                    """;

                try (PreparedStatement statement =
                             connection.prepareStatement(conversationSql)) {
                    statement.setString(1, sessionId);
                    statement.setString(2, now);
                    statement.setString(3, now);
                    statement.executeUpdate();
                }

                // Save the message.
                String messageSql = """
                    INSERT INTO messages
                        (conversation_id, role, content, created_at)
                    VALUES (?, ?, ?, ?)
                    """;

                try (PreparedStatement statement =
                             connection.prepareStatement(messageSql)) {
                    statement.setString(1, sessionId);
                    statement.setString(2, role);
                    statement.setString(3, content);
                    statement.setString(4, now);
                    statement.executeUpdate();
                }

                // Set the title from the first user message only.
                if ("user".equals(role)) {
                    String titleSql = """
                        UPDATE conversations
                        SET title = ?
                        WHERE id = ?
                          AND title = 'New Chat'
                        """;

                    try (PreparedStatement statement =
                                 connection.prepareStatement(titleSql)) {
                        statement.setString(1, content);
                        statement.setString(2, sessionId);
                        statement.executeUpdate();
                    }
                }

                // Update the conversation timestamp.
                String updateSql = """
                    UPDATE conversations
                    SET updated_at = ?
                    WHERE id = ?
                    """;

                try (PreparedStatement statement =
                             connection.prepareStatement(updateSql)) {
                    statement.setString(1, now);
                    statement.setString(2, sessionId);
                    statement.executeUpdate();
                }

                connection.commit();

            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to save chat message", e
            );
        }
    }

    public List<Map<String, String>> getChatHistory(
            String sessionId) {

        List<Map<String, String>> history = new ArrayList<>();

        String sql = """
            SELECT role, content
            FROM (
                SELECT id, role, content
                FROM messages
                WHERE conversation_id = ?
                ORDER BY id DESC
                LIMIT ?
            )
            ORDER BY id ASC
            """;

        try (Connection connection = getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, sessionId);
            statement.setInt(2, MAX_HISTORY_MESSAGES);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Map<String, String> message = new HashMap<>();
                    message.put("role", result.getString("role"));
                    message.put("content", result.getString("content"));
                    history.add(message);
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to retrieve chat history", e
            );
        }

        return history;
    }

    public List<Map<String, String>> getConversationMessages(
            String sessionId) {

        List<Map<String, String>> messages = new ArrayList<>();

        String sql = """
            SELECT id, role, content, created_at
            FROM messages
            WHERE conversation_id = ?
            ORDER BY id ASC
            """;

        try (Connection connection = getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, sessionId);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Map<String, String> message = new HashMap<>();
                    message.put("id", result.getString("id"));
                    message.put("role", result.getString("role"));
                    message.put("content", result.getString("content"));
                    message.put(
                            "created_at",
                            result.getString("created_at")
                    );
                    messages.add(message);
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to retrieve conversation messages", e
            );
        }

        return messages;
    }

    public List<Map<String, String>> listConversations() {
        List<Map<String, String>> conversations = new ArrayList<>();

        String sql = """
            SELECT c.id AS session_id,
                   c.title,
                   (
                       SELECT m.content
                       FROM messages m
                       WHERE m.conversation_id = c.id
                       ORDER BY m.id DESC
                       LIMIT 1
                   ) AS preview
            FROM conversations c
            ORDER BY c.updated_at DESC
            """;

        try (Connection connection = getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                Map<String, String> conversation = new HashMap<>();
                conversation.put(
                        "session_id",
                        result.getString("session_id")
                );
                conversation.put("title", result.getString("title"));
                conversation.put("preview", result.getString("preview"));
                conversations.add(conversation);
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to list conversations", e
            );
        }

        return conversations;
    }
}
