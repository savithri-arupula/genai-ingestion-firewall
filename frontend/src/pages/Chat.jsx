import React from "react";

import { useCallback, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { AlertTriangle, X } from "lucide-react";

import ChatHeader from "../components/ChatHeader.jsx";
import Composer from "../components/Composer.jsx";
import EmptyState from "../components/EmptyState.jsx";
import InsightsPanel from "../components/InsightsPanel.jsx";
import MessageList from "../components/MessageList.jsx";
import Sidebar from "../components/Sidebar.jsx";
import { useBackendStatus } from "../hooks/useBackendStatus.js";
import { api } from "../lib/api.js";

const newId = () => crypto.randomUUID();

export default function Chat() {
  const navigate = useNavigate();
  const status = useBackendStatus();

  const [messages, setMessages] = useState([]);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  const [sidebarHidden, setSidebarHidden] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const activeSessionRef = useRef(null);

  const sendMessage = useCallback(
    async (text) => {
      const message = text.trim();
      if (!message || pending) return;

      const userMessage = {
        id: newId(),
        role: "user",
        content: message,
        createdAt: new Date().toISOString(),
      };

      setMessages((current) => [...current, userMessage]);
      setError("");
      setPending(true);

      try {
        const data = await api.analyze(message);
        const {
          response_markdown: content,
          ...analysis
        } = data;

        setMessages((current) => [
          ...current,
          {
            id: newId(),
            role: "assistant",
            content: content || "No response was returned.",
            createdAt: new Date().toISOString(),
            analysis,
          },
        ]);
      } catch (err) {
        setMessages((current) =>
          current.map((item) =>
            item.id === userMessage.id
              ? { ...item, failed: true }
              : item
          )
        );

        setError(err.message || "Something went wrong.");
      } finally {
        setPending(false);
      }
    },
    [pending]
  );

  const startNewChat = () => {
    setMessages([]);
    setError("");
    setMobileMenuOpen(false);
    navigate("/chat");
  };

  const latestAnalysis = useMemo(
    () =>
      [...messages]
        .reverse()
        .find((item) => item.analysis)?.analysis ?? null,
    [messages]
  );

  const title =
    messages[0]?.content.slice(0, 60) || "New Conversation";

  const showEmptyState = messages.length === 0 && !pending;

  return (
    <div
      className={`chat-layout ${
        sidebarHidden ? "sidebar-hidden" : ""
      } ${mobileMenuOpen ? "menu-open" : ""}`}
    >
      <Sidebar
        conversations={[]}
        status={status}
        onNewChat={startNewChat}
        onCollapse={() => setSidebarHidden(true)}
        onNavigate={() => setMobileMenuOpen(false)}
      />

      <button
        type="button"
        className="sidebar-scrim"
        aria-label="Close menu"
        onClick={() => setMobileMenuOpen(false)}
      />

      <main className="chat-main">
        <ChatHeader
          title={title}
          status={status}
          sidebarHidden={sidebarHidden}
          onShowSidebar={() => setSidebarHidden(false)}
          onOpenMenu={() => setMobileMenuOpen(true)}
          onNewChat={startNewChat}
        />

        <div className="chat-scroll">
          {showEmptyState ? (
            <EmptyState onPick={sendMessage} disabled={pending} />
          ) : (
            <MessageList messages={messages} pending={pending} />
          )}
        </div>

        <div className="composer-wrap">
          {error && (
            <div className="error-banner" role="alert">
              <AlertTriangle size={16} />
              <span>{error}</span>
              <button
                type="button"
                className="icon-btn"
                onClick={() => setError("")}
                aria-label="Dismiss error"
              >
                <X size={16} />
              </button>
            </div>
          )}

          <Composer
            onSend={sendMessage}
            disabled={pending}
            placeholder={
              messages.length
                ? "Ask a follow-up question…"
                : "Ask anything…"
            }
          />

          <p className="disclaimer">
            AI responses may not always be accurate. Please verify important
            information.
          </p>
        </div>
      </main>

      <InsightsPanel
        analysis={latestAnalysis}
        onAsk={sendMessage}
        disabled={pending}
      />
    </div>
  );
}
