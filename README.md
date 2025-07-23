# Chatbot Tesla - Cloud Humans Take Home

This project was developed as part of a technical challenge by Cloud Humans. The goal is to implement an application based on **RAG (Retrieval-Augmented Generation)** architecture and **LLM (Large Language Model)** to answer user questions using contextualized data.

The implementation uses **Spring Boot**, integration with external APIs (embedding, vector database, and OpenAI), and **Response Rule #2**, known as the **Handover Feature**, where the system determines if the response should be handed off to a human agent.

---

## Technologies Used

- Java 17
- Spring Boot 3
- Docker and Docker Compose
- OpenAI API (Chat and Embeddings)
- RAG Architecture (Retrieval-Augmented Generation)
- JUnit 5 + Mockito for unit testing
- Log4j2 for logging
- Maven as dependency manager

---

## How to Run the Project

### Prerequisites

- Docker and Docker Compose installed
- Java 17 (only if running without Docker)
- OpenAI API key
- VectorDB API key

---

### In the project root, there is a `.env` file where you must add your actual OpenAI and VectorDB keys:

> **Note:** Real keys are not provided in this repository for security reasons. Fill it in with your own credentials.

### Run with Docker
```
docker compose up --build
```

### The API will be available at:
```
http://localhost:8080/chat
```

## Request Structure
```
  curl -X POST http://localhost:8080/chat \
  -H "Content-Type: application/json" \
  -d '{
    "projectName": "tesla_motors",
    "messages": [
      {
        "role": "USER",
        "content": "How long does a Tesla battery last before it needs to be replaced?"
      }
    ]
  }'
```
  
## Unit Tests

All tests are located at:  
  `src/test/java/com/cloudhumans/chatbot/service/ChatServiceTest.java`

They validate the complete flow of the **RAG (Retrieval-Augmented Generation)** architecture:

- Embedding vector creation  
- Query to the vector database  
- Query to OpenAI  
- Response with potential **human handover** activation

---

## RAG Architecture and Response Logic

The project follows the **RAG (Retrieval-Augmented Generation)** architecture with the following steps:

1. The user's question is converted into a vector via the **embedding** endpoint.
2. The vector is sent to the **vector database**, returning the most relevant documents.
3. These documents are sent to the **OpenAI model** along with the original question.
4. The final response is analyzed:
   - If the returned document type is `"N2"` (non-critical), the system **activates the handover** to a human agent.
   - Otherwise, the response is returned directly to the user.

This logic implements **Option 2** of the Cloud Humans challenge, also known as the **Handover Feature**.

## Notes

- The `.env` file was intentionally included in the repository with blank keys since this is a technical challenge.
- In real-world environments, this file should be listed in `.gitignore`.
- All external endpoints were mocked in the unit tests.

## Author
Developed by **Edvan Junior** as part of the **Cloud Humans** selection process.
