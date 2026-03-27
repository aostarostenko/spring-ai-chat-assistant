# 🛠️ Troubleshooting Guide

This document covers common issues encountered while running the **AI Chat Assistant** with Spring AI 1.1.2 and Google Gemini.

## 🔑 1. Google Gemini API Errors

### **Error: `429 Too Many Requests`**
* **Cause**: You have exceeded your current quota (Requests Per Minute or Tokens Per Day).
* **Fix**:
    1. Check [Google AI Studio Dashboard](https://aistudio.google.com/).
    2. Ensure your project is in **Tier 1** (Link a billing account). Even if you stay in the free usage range, Tier 1 provides a much higher RPM than the "Unverified Free" tier.
    3. Implement a **Retry Template** in your Spring configuration to handle transient rate limits.

### **Error: `404 Model Not Found`**
* **Cause**: The requested model ID (e.g., `gemini-1.5-flash`) has been deprecated or retired in 2026.
* **Fix**: Update `application.yml` to a current stable model:
    * **Recommended**: `gemini-2.5-flash`
    * **Advanced**: `gemini-3.1-pro-preview`

### **Error: `Google GenAI project-id must be set!`**
* **Cause**: The Spring AI Google GenAI implementation requires an explicit Project ID to manage billing and quotas.
* **Fix**:
    1. Locate your Project ID in Google AI Studio (e.g., `gen-lang-client-xxxxxxxx`).
    2. Add `spring.ai.google.genai.project-id: ${GOOGLE_PROJECT_ID}` to your `application.yml`.
    3. Ensure the variable is passed in your `docker-compose.yml` environment.

---

## 🏗️ 2. Infrastructure & Startup Issues

### **Application Fails to Start (Context Initialization)**
* **Check YAML Indentation**: Ensure the `ai:` block is **not** nested inside the `mongodb:` block. It must be a direct child of `spring:`.
* **Stale Docker Build**: If you changed `application.yml`, Docker might be using an old image. Run:
    ```bash
    docker-compose down
    docker-compose up --build -d
    ```

### **WebSocket Handshake Fails (404 at `/chat-websocket`)**
* **Cause**: Incorrect Context Path mapping.
* **Fix**: Since the app runs at `/ai-chat-assistant/`, ensure your `SockJS` connection in `chat.html` uses the dynamic Thymeleaf path:
    ```javascript
    const socket = new SockJS([[ @{/chat-websocket} ]]);
    ```

---

## 🎨 3. UI & Readability Issues

### **AI Responses Look Like Raw Code**
* **Cause**: Markdown is not being parsed on the client side.
* **Fix**: Ensure **Marked.js** is included in your `chat.html` and that `showMessage()` uses `marked.parse(message.content)` instead of `.textContent`.

### **Sidebar Always Shows "New Chat"**
* **Cause**: Summarization service failed or hasn't triggered.
* **Fix**:
    1. Check logs for `RuntimeException: Failed to generate content` (usually a model 404).
    2. Ensure the `summarizeAsync` trigger in `ChatService` is set to `count == 2` for immediate results.
    3. Verify the WebSocket subscription for `/topic/session-update/{sessionId}` is active.

---

## 📝 4. Diagnostic Commands

**List available models for your API Key:**
```bash
curl "https://generativelanguage.googleapis.com/v1/models?key=$GEMINI_API_KEY"
```

**Check MongoDB Connectivity inside Docker:**
```bash
docker exec -it spring-ai-chat-db mongosh --eval "db.adminCommand('ping')"
```

---