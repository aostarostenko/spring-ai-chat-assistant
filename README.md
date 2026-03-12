# 🤖 AI Chat Assistant

### *Enterprise AI Chat with Spring Boot 4.0.3 & Java 25*

**AI Chat Assistant** is a high-performance, real-time AI conversation platform. Built with **Hexagonal (Ports & Adapters) Architecture**, it ensures business logic remains decoupled from infrastructure like MongoDB and Google Gemini 3 Flash.

---

## 🛠 Technology Stack

* **Java 25 (LTS)**: High-throughput concurrency via **Virtual Threads**.
* **Spring Boot 4.0.3 & Spring AI 1.1.2**: Modern AI integration.
* **Google Gemini 3 Flash**: Ultra-low latency reasoning.
* **MongoDB 8.0+**: Flexible document persistence.
* **WebSockets (STOMP)**: Full-duplex real-time streaming.

---

## 📖 How to Use the App

### **1. Getting Started**

1. **Access the App**: Navigate to `http://localhost:8080` in your browser.
2. **Language Selection**: By default, the app loads in **Ukrainian**. To switch to **English**, click the "English" link in the footer or append `?lang=en` to the URL.
3. **Registration**: Click "Sign Up" and provide a username, email, and password. If the email is already taken, a localized `WebException` message will guide you.

### **2. Starting a Chat**

1. **New Session**: Click the **"+ New Chat"** button in the sidebar.
2. **Sending Messages**: Type your prompt into the message box at the bottom.
3. **Real-Time Response**: Watch as Gemini 3 Flash streams the response back to you. You don't need to refresh the page; the WebSocket connection handles the updates.
4. **Auto-Summarization**: After 5 messages, the system uses a **Virtual Thread** to generate a 6-word title for your session, which will automatically update in your sidebar history.

### **3. Managing Your Profile**

1. **Account Settings**: Click on your profile icon to change your email or update your password.
2. **Security Errors**: If you enter an incorrect old password, a `SecurityException` will trigger a secure redirect to the login page with a specialized warning.

### **4. Error Feedback**

* If the AI service is busy, a toast notification or error banner will appear with the message mapped from `WEB_103`.
* If the database is down, you will be redirected to the home screen with a `DAO_001` error message.

---

## 🏗️ System Structure (Hexagonal)

1. **Domain**: Core business logic (Records & Interfaces).
2. **Application**: Service orchestration using Virtual Threads.
3. **Infrastructure**: Technical implementations (DB, Web, Security, AI).

---

## 🏗️ Folder Structure

```text
src/main/java/com/nexus/chatassistant/
├── application/         # Use Case Orchestration (The "Brain")
│   └── service/         # ChatService, UserService, SummarizationService
├── domain/              # Business Logic & Models (The "Core")
│   ├── model/           # Records: User, ChatMessage, ChatSession
│   ├── repository/      # Ports: Repository Interfaces
│   └── exception/       # Custom Exception Hierarchy & ErrorCodes constants
└── infrastructure/      # Adapters (The "Tools")
    ├── persistence/     # MongoDB Implementations
    ├── web/             # Controllers, GlobalExceptionHandler
    ├── security/        # Spring Security 6 & BCrypt
    └── config/          # Locale, WebSocket, and App Configurations

```
---

## 🛡️ Error Handling Architecture

The application implements a centralized, multi-tier error strategy using **Domain Exceptions** and **Localized Error Codes**.

### **The Three-Tier Exception Model**

1. **`WebException`**: Business validation (e.g., `WEB_101`: Duplicate User).
2. **`SecurityException`**: Auth & Authorization (e.g., `SEC_003`: User Not Found).
3. **`DaoException`**: Persistence failures (e.g., `DAO_001`: DB Connection Lost).

---

## 🛠️ Quick Start

```bash
# 1. Set your Gemini API Key
export GEMINI_API_KEY="your_key"

# 2. Spin up the environment
docker-compose up --build

```
