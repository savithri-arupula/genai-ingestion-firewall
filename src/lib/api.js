const BASE_URL = "http://localhost:8080";
async function request(path, options = {}) {
  let response;

  try {
    response = await fetch(`${BASE_URL}${path}`, {
      headers: {
        "Content-Type": "application/json",
      },
      ...options,
    });
  } catch {
    throw new Error(
      "Cannot reach the backend. Is Spring Boot running on port 8080?"
    );
  }

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    throw new Error(
      data?.detail ||
        data?.message ||
        data?.error ||
        data?.title ||
        `Request failed (${response.status}).`
    );
  }

  return data;
}

export const api = {
  health: () => request("/health"),

  listConversations: () =>
    request("/api/conversations").then((data) => data.items),

  getMessages: (sessionId) =>
    request(`/api/conversations/${sessionId}/messages`).then(
      (data) => data.items
    ),

  analyze: (message, sessionId) =>
    request("/api/analyze", {
      method: "POST",
      body: JSON.stringify(
        sessionId
          ? { message, session_id: sessionId }
          : { message }
      ),
    }),
};