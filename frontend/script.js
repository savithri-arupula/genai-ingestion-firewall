
const API_URL = "http://localhost:8080/api/analyze";
const HEALTH_URL = "http://localhost:8080/health";

const chatForm = document.getElementById("chat-form");
const messageInput = document.getElementById("message-input");
const sendButton = document.getElementById("send-button");
const chatMessages = document.getElementById("chat-messages");
const chatTitle = document.getElementById("chat-title");
const welcome = document.getElementById("welcome");
const errorMessage = document.getElementById("error-message");
const backendStatus = document.getElementById("backend-status");
const statusDot = document.getElementById("status-dot");
const newChatButton = document.getElementById("new-chat");

let isLoading = false;
let hasMessages = false;

// Escape text if Markdown libraries are unavailable.
function escapeHTML(value) {
    return String(value).replace(/[&<>"']/g, character => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#039;"
    })[character]);
}

// Render Markdown safely.
function renderMarkdown(markdown) {
    const text = String(markdown ?? "");

    if (
        typeof marked === "undefined" ||
        typeof DOMPurify === "undefined"
    ) {
        return `<pre>${escapeHTML(text)}</pre>`;
    }

    const html = marked.parse(text);
    return DOMPurify.sanitize(html);
}

function scrollToBottom() {
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

function updateBackendStatus(connected, message) {
    backendStatus.textContent = message;
    statusDot.classList.toggle("connected", connected);
    statusDot.classList.toggle("disconnected", !connected);
}

function showError(message) {
    errorMessage.textContent = message;
    errorMessage.hidden = false;
}

function clearError() {
    errorMessage.textContent = "";
    errorMessage.hidden = true;
}

function addUserMessage(message) {
    welcome.hidden = true;

    const element = document.createElement("div");
    element.className = "message user-message";
    element.textContent = message;

    chatMessages.appendChild(element);
    scrollToBottom();
}

function addLoadingMessage() {
    const element = document.createElement("div");
    element.className = "message loading";
    element.id = "loading-message";
    element.textContent = "AI is thinking...";

    chatMessages.appendChild(element);
    scrollToBottom();

    return element;
}

// Display the AI response and optional classification details.
function addAssistantMessage(markdown, intentData) {
    const wrapper = document.createElement("div");
    wrapper.className = "message assistant-message";

    const label = document.createElement("div");
    label.className = "assistant-label";
    label.textContent = "✦ AI Assistant";

    const content = document.createElement("div");
    content.className = "assistant-content";
    content.innerHTML = renderMarkdown(markdown);

    wrapper.append(label, content);

    if (intentData && Object.values(intentData).some(
        value => value !== undefined && value !== null
    )) {
        const details = document.createElement("details");
        details.className = "intent-details";

        const summary = document.createElement("summary");
        summary.textContent = "Detected Intent & Analysis";

        const pre = document.createElement("pre");
        pre.textContent = JSON.stringify(intentData, null, 2);

        details.append(summary, pre);
        wrapper.appendChild(details);
    }

    chatMessages.appendChild(wrapper);
    scrollToBottom();
}

// Read JSON safely, even if the server returns plain text or HTML.
async function readResponse(response) {
    const text = await response.text();

    if (!text) {
        return {};
    }

    try {
        return JSON.parse(text);
    } catch {
        return { message: text };
    }
}

// Support several common backend response formats.
function getAssistantText(data) {
    const candidates = [
        data.response_markdown,
        data.response,
        data.answer,
        data.message,
        data.result
    ];

    for (const candidate of candidates) {
        if (typeof candidate === "string" && candidate.trim()) {
            return candidate;
        }
    }

    // Some APIs return a nested response object.
    if (data.result && typeof data.result === "object") {
        const nested = getAssistantText(data.result);

        if (nested) {
            return nested;
        }
    }

    // If the backend returns classification fields only,
    // show them instead of leaving the chat blank.
    if (
        data.intent !== undefined ||
        data.summary !== undefined ||
        data.priority !== undefined
    ) {
        return "Message analyzed successfully.\n\n" +
            "```json\n" +
            JSON.stringify(data, null, 2) +
            "\n```";
    }

    return null;
}

function getErrorText(data, status) {
    if (typeof data.detail === "string") {
        return data.detail;
    }

    if (typeof data.error === "string") {
        return data.error;
    }

    if (typeof data.message === "string") {
        return data.message;
    }

    return `The server returned an error (${status}).`;
}

async function sendMessage(message) {
    const trimmedMessage = message.trim();

    if (isLoading || !trimmedMessage) {
        return;
    }

    isLoading = true;
    sendButton.disabled = true;
    messageInput.disabled = true;
    newChatButton.disabled = true;
    clearError();

    addUserMessage(trimmedMessage);

    if (!hasMessages) {
        chatTitle.textContent = trimmedMessage.length > 32
            ? `${trimmedMessage.slice(0, 32)}...`
            : trimmedMessage;

        hasMessages = true;
    }

    const loadingElement = addLoadingMessage();

    try {
        const response = await fetch(API_URL, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Accept": "application/json"
            },
            body: JSON.stringify({
                message: trimmedMessage
            })
        });

        const data = await readResponse(response);

        if (!response.ok) {
            throw new Error(getErrorText(data, response.status));
        }

        const assistantText = getAssistantText(data);

        if (!assistantText) {
            throw new Error(
                "The backend returned no recognizable response text. " +
                "Check the JSON returned by /api/analyze."
            );
        }

        const intentData = {
            intent: data.intent,
            risk_profile: data.risk_profile,
            priority: data.priority,
            requires_action: data.requires_action,
            summary: data.summary
        };

        const hasIntentData = Object.values(intentData).some(
            value => value !== undefined && value !== null
        );

        addAssistantMessage(
            assistantText,
            hasIntentData ? intentData : null
        );

        updateBackendStatus(true, "Backend Connected");
    } catch (error) {
        const message = error instanceof TypeError
            ? "Could not connect to the Java backend. " +
              "Check that Spring Boot is running on port 8080 " +
              "and that CORS allows your frontend."
            : error.message;

        showError(message);
        updateBackendStatus(false, "Backend Disconnected");
    } finally {
        loadingElement.remove();
        isLoading = false;
        sendButton.disabled = false;
        messageInput.disabled = false;
        newChatButton.disabled = false;
        messageInput.focus();
    }
}

// Submit the chat form.
chatForm.addEventListener("submit", event => {
    event.preventDefault();

    if (isLoading) {
        return;
    }

    const message = messageInput.value.trim();

    if (!message) {
        messageInput.focus();
        return;
    }

    messageInput.value = "";
    sendMessage(message);
});

// Enter sends; Shift + Enter adds a new line.
messageInput.addEventListener("keydown", event => {
    if (
        event.key === "Enter" &&
        !event.shiftKey &&
        !event.isComposing
    ) {
        event.preventDefault();
        chatForm.requestSubmit();
    }
});

// Suggested prompts.
document.querySelectorAll(".suggestion").forEach(button => {
    button.addEventListener("click", () => {
        if (isLoading) {
            return;
        }

        sendMessage(button.textContent.trim());
    });
});

// Start a fresh conversation in the UI.
newChatButton.addEventListener("click", () => {
    if (isLoading) {
        return;
    }

    chatMessages.replaceChildren();
    chatMessages.appendChild(welcome);

    welcome.hidden = false;
    chatTitle.textContent = "New Chat";
    hasMessages = false;
    messageInput.value = "";

    clearError();
    messageInput.focus();
});

// Check whether the Java backend is reachable.
// This endpoint must exist in your Spring Boot application.
async function checkBackend() {
    try {
        const response = await fetch(HEALTH_URL, {
            method: "GET",
            headers: {
                "Accept": "application/json"
            }
        });

        if (!response.ok) {
            throw new Error("Health endpoint returned an error");
        }

        updateBackendStatus(true, "Backend Connected");
    } catch {
        updateBackendStatus(
            false,
            "Backend Not Connected"
        );
    }
}

checkBackend();
