This is the **final, comprehensive README** for your project. It’s designed to be the "source of truth" for anyone (including your future self) who needs to run, maintain, or explain this architecture.

---

# 🤖 Nexus Chat Assistant

### *Spring AI + MongoDB + Hexagonal Architecture*

A production-ready AI Chat application built with **Spring Boot 4.0.3** and **Spring AI 1.1.2**. This project leverages **Google Gemini 3** to provide real-time conversational intelligence while maintaining a clean, decoupled **Hexagonal Architecture**.

---

## 🏗️ Architecture: The "Hexagon"

This project follows the **Ports and Adapters** pattern to ensure that the business logic (the chat flow) is independent of external technologies (the AI provider, the DB, or the UI).

### **Folder Structure Breakdown**

* **`domain`**: The core. Contains pure Java records (`ChatMessage`, `ChatSession`) and repository interfaces (Ports). No framework dependencies.
* **`application`**: The "Brain." Orchestrates use cases like starting a chat, generating AI responses, and auto-summarizing sessions.
* **`infrastructure`**: The "Adapters."
* **`persistence`**: MongoDB implementation of our repositories.
* **`web`**: WebSocket (STOMP) and REST controllers for the frontend.
* **`security`**: Spring Security 6 configurations and BCrypt password handling.
* **`ai`**: Integration with Google Gemini 3 via Spring AI.



---

## 🚀 Tech Stack

* **Java 25**: Leveraging the latest LTS features and Virtual Threads.
* **Spring Boot 3.4.3**: The backbone of the application.
* **Spring AI 1.1.2 (GA)**: Using the latest stable General Availability release.
* **Google Gemini 3 Flash**: High-speed, high-reasoning flagship model.
* **MongoDB**: Flexible document storage for chat history.
* **WebSockets (STOMP)**: For low-latency, real-time message streaming.
* **Thymeleaf**: Server-side rendering for a snappy, secure UI.

---

## 🛠️ Installation & Setup

### 1. Prerequisites

* **JDK 25** installed.
* **MongoDB** running (Local or Atlas).
* **Google AI API Key** (Get it from [Google AI Studio](https://aistudio.google.com/)).

### 2. Environment Variables

Create a `.env` file or set these in your system:

```bash
GEMINI_API_KEY=your_api_key_here
MONGODB_URI=mongodb://localhost:27017/chat_assistant

```

### 3. Build the Project

Use the provided Maven Wrapper to force-refresh dependencies:

```powershell
./mvnw clean install -U -DskipTests

```

### 4. Run the App

```powershell
./mvnw spring-boot:run

```

Access the app at: `http://localhost:8080`

---

## 📡 API & WebSocket Routes

| Type | Route | Description |
| --- | --- | --- |
| **GET** | `/` | Main Chat Interface (Login required) |
| **POST** | `/register` | User Registration |
| **WS** | `/app/chat/{id}` | Inbound: Send message to session |
| **WS** | `/topic/messages/{id}` | Outbound: Receive real-time AI response |

---

## 🛡️ Security Features

* **Session Isolation**: Users can only see and interact with their own chat sessions.
* **CSRF Protection**: Configured specifically to allow WebSocket handshakes while keeping REST routes safe.
* **Encrypted Storage**: Passwords are never stored in plain text (BCrypt).