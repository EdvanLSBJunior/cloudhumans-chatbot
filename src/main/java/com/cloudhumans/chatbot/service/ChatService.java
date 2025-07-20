package com.cloudhumans.chatbot.service;

import com.cloudhumans.chatbot.model.embedding.EmbeddingRequest;
import com.cloudhumans.chatbot.model.embedding.EmbeddingResponse;
import com.cloudhumans.chatbot.model.llm.ChatCompletionRequest;
import com.cloudhumans.chatbot.model.llm.ChatCompletionResponse;
import com.cloudhumans.chatbot.model.llm.Message;
import com.cloudhumans.chatbot.model.response.ConversationResponse;
import com.cloudhumans.chatbot.model.search.DatabaseSearchResponse;
import com.cloudhumans.chatbot.model.search.SearchResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final Logger logger = LogManager.getLogger(ChatService.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${cloudhumans.embedding.url}")
    private String embeddingApiUrl;

    @Value("${cloudhumans.embedding.api-key}")
    private String embeddingApiKey;

    @Value("${cloudhumans.embedding.model}")
    private String embeddingModel;

    @Value("${database.search.url}")
    private String dbSearchUrl;

    @Value("${database.search.api-key}")
    private String dbApiKey;

    @Value("${openai.chat.url}")
    private String openAiChatUrl;

    @Value("${openai.chat.api-key}")
    private String openAiApiKey;

    @Value("${openai.chat.model}")
    private String openAiModel;

    public ConversationResponse getAnswer(String projectName, String userMessage) {
        List<Double> vector = fetchEmbeddingVector(userMessage);
        if (vector == null) {
            return new ConversationResponse(
                    List.of(
                            new Message("USER", userMessage),
                            new Message("AGENT", "Erro ao gerar embedding.")
                    ),
                    true,
                    List.of()
            );
        }

        List<SearchResult> results = queryDatabase(vector, projectName);
        if (results == null || results.isEmpty()) {
            return new ConversationResponse(
                    List.of(
                            new Message("USER", userMessage),
                            new Message("AGENT", "Desculpe, não encontrei uma resposta para sua pergunta.")
                    ),
                    true,
                    List.of()
            );
        }

        String context = results.stream()
                .map(SearchResult::getContent)
                .reduce("", (a, b) -> a + "\n" + b);

        String llmResponse = callGpt4(userMessage, context);
        boolean shouldEscalate = llmResponse != null && llmResponse.toLowerCase().contains("i will escalate");
        boolean hasN2 = results.stream().anyMatch(r -> "N2".equalsIgnoreCase(r.getType()));

        return new ConversationResponse(
                List.of(
                        new Message("USER", userMessage),
                        new Message("AGENT", llmResponse != null ? llmResponse : "Erro ao gerar resposta via LLM.")
                ),
                hasN2 || shouldEscalate,
                results
        );
    }

    private List<Double> fetchEmbeddingVector(String input) {
        EmbeddingRequest request = new EmbeddingRequest(input, embeddingModel);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(embeddingApiKey);

        HttpEntity<EmbeddingRequest> entity = new HttpEntity<>(request, headers);

        logger.info("Enviando texto para embedding API...");

        try {
            ResponseEntity<String> rawResponse = restTemplate.exchange(
                    embeddingApiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );
            logger.info("Embedding gerado com sucesso.");
            EmbeddingResponse response = objectMapper.readValue(rawResponse.getBody(), EmbeddingResponse.class);
            return response.getFirstEmbedding();
        } catch (Exception e) {
            logger.error("Erro ao chamar a API de embeddings:", e);
            return null;
        }
    }

    private List<SearchResult> queryDatabase(List<Double> vector, String projectName) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", dbApiKey);

        String vectorJson;
        try {
            vectorJson = objectMapper.writeValueAsString(vector);
        } catch (Exception e) {
            logger.error("Erro ao converter vetor para JSON:", e);
            return null;
        }

        String requestBody = String.format("""
                {
                  "count": true,
                  "select": "content, type",
                  "top": 10,
                  "filter": "projectName eq '%s'",
                  "vectorQueries": [
                    {
                      "vector": %s,
                      "k": 10,
                      "fields": "embeddings",
                      "kind": "vector"
                    }
                  ]
                }
                """, projectName, vectorJson);

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> rawResponse = restTemplate.exchange(
                    dbSearchUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );
            logger.info("Resultados recuperados do vector DB.");
            DatabaseSearchResponse typedResponse = objectMapper.readValue(rawResponse.getBody(), DatabaseSearchResponse.class);
            return typedResponse.getValue();
        } catch (Exception e) {
            logger.error("Erro ao consultar o vector DB:", e);
            return null;
        }
    }

    private String callGpt4(String userMessage, String context) {
        List<Message> messages = List.of(
                new Message("system", """
                        You are a Tesla support assistant. 
                        Only answer questions using the provided context. 
                        If the answer is not explicitly mentioned in the context, respond with:
                        "I'm sorry, I couldn't find this information in our records. I will escalate this request to a human assistant."

                        Never use external or general knowledge, even if you know the answer.
                        """),
                new Message("user", "Context:\n" + context + "\n\nQuestion: " + userMessage)
        );

        ChatCompletionRequest request = new ChatCompletionRequest(openAiModel, messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        HttpEntity<ChatCompletionRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<ChatCompletionResponse> response = restTemplate.exchange(
                    openAiChatUrl,
                    HttpMethod.POST,
                    entity,
                    ChatCompletionResponse.class
            );
            return response.getBody().getChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            logger.error("Erro ao chamar o modelo GPT-4:", e);
            return null;
        }
    }
}