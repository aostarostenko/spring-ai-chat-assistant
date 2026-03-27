# 🤖 AI Chat Assistant

### *Enterprise AI Chat with Spring Boot 4.0.3 & Java 25*

**AI Chat Assistant** is a high-performance, real-time AI conversation platform. Built with **Hexagonal (Ports & Adapters) Architecture**, it ensures business logic remains decoupled from infrastructure like MongoDB and Google Gemini 2.5/3.1 Flash.

---

## 🛠 Technology Stack

* [cite_start]**Java 25 (LTS)**: High-throughput concurrency via **Virtual Threads** (Project Loom). [cite: 483]
* [cite_start]**Spring Boot 4.0.3 & Spring AI 1.1.2**: Enterprise AI integration and auto-configuration. [cite: 481, 484]
* **Google Gemini 2.5 Flash**: Ultra-low latency reasoning (Tier 1 API support).
* [cite_start]**MongoDB 8.0+**: Flexible document persistence for chat history. [cite: 485]
* [cite_start]**WebSockets (STOMP)**: Full-duplex real-time streaming for messages and sidebar updates. [cite: 485]
* **Marked.js**: Client-side Markdown parsing for formatted AI responses (bold, code blocks, lists).

---

## 📖 How to Use the App

### **1. Getting Started**

1.  **Access the App**: Navigate to `http://localhost:8080/ai-chat-assistant/` in your browser.
2.  **Language Selection**: If enabled in configuration, you can switch between **Ukrainian** and **English** via the footer links.
3.  **Registration**: Click "Sign Up" to create an account. [cite_start]If the email is taken, a localized `WebException` (`WEB_101`) will guide you. [cite: 489, 504]

### **2. Starting a Chat**

1.  **New Session**: Click **"+ New Chat"** in the sidebar. This generates a unique session ID and redirects you to the context-aware chat interface.
2.  **Real-Time Formatting**: AI responses are rendered in real-time. Code snippets, bold text, and lists are formatted via Markdown for maximum readability.
3.  **Instant Auto-Summarization**: After the **first exchange** (1 user message + 1 AI response), the system triggers a background Virtual Thread to generate a 6-word title.
4.  **Sidebar Sync**: Titles update dynamically in the sidebar via WebSockets without requiring a page refresh.

### **3. Managing Your Profile**

1.  **Account Settings**: Change your display name, email, or password.
2.  [cite_start]**Security Errors**: Incorrect password attempts trigger a `SecurityException` (`SEC_005`), redirecting you with a secure warning. [cite: 497, 118]

---

## ⚙️ Configuration & Features

### **Environment Variables**
To run the AI features, the following variables must be provided to the Docker environment:
* `GEMINI_API_KEY`: Your Google AI Studio API Key (Tier 1 recommended).
* `GOOGLE_PROJECT_ID`: Your Google Cloud Project ID (e.g., `gen-lang-client-xxxx`).

### **Feature Flags**
In `application.yml`, you can toggle system features:
* `app.features.multi-language-enabled`: Set to `false` to hide language switchers and lock the UI to the default locale.

---

## 🏗️ System Structure (Hexagonal)

1.  [cite_start]**Domain**: Core business logic, Entities (Records), and Repository Ports. [cite: 501]
2.  [cite_start]**Application**: Service orchestration (Chat, User, Summarization) using Virtual Threads. [cite: 501]
3.  [cite_start]**Infrastructure**: Adapters for MongoDB, Spring Security 6, WebSockets, and Gemini AI. [cite: 501]

---

## 🛡️ Error Handling Architecture

[cite_start]The application implements a centralized, multi-tier error strategy using **Domain Exceptions** and **Localized Error Codes**. [cite: 503]

1.  [cite_start]**`WebException`**: Business validation (e.g., `WEB_101`: Duplicate User). [cite: 504]
2.  [cite_start]**`SecurityException`**: Auth & Authorization (e.g., `SEC_003`: User Not Found). [cite: 505]
3.  [cite_start]**`DaoException`**: Persistence failures (e.g., `DAO_001`: DB Connection Lost). [cite: 505]

---

## 🛠️ Quick Start

```bash
# 1. Set your environment variables (PowerShell example)
$env:GEMINI_API_KEY="your_key"
$env:GOOGLE_PROJECT_ID="gen-lang-client-xxxxxxxx"

# 2. Spin up the environment
docker-compose up --build -d

# 3. Access the app
# URL: http://localhost:8080/ai-chat-assistant/
```

### **Rebuilding Containers**
If you modify `application.yml` or Java code, you must rebuild the image:
```bash
# Stop and remove containers
docker-compose down

# Rebuild and start
docker-compose up --build -d
```

---