package com.cloudhumans.chatbot.service;

import com.cloudhumans.chatbot.model.embedding.EmbeddingRequest;
import com.cloudhumans.chatbot.model.embedding.EmbeddingResponse;
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

    public String getAnswer(String question) {
        List<Double> vector = fetchEmbeddingVector(question);
        if (vector == null) {
            return "Erro ao gerar embedding.";
        }

        List<SearchResult> results = queryDatabase(vector);
        if (results == null || results.isEmpty()) {
            return "Desculpe, não encontrei uma resposta para sua pergunta.";
        }

        return results.stream()
                .max(Comparator.comparingDouble(SearchResult::getSearchScore))
                .map(SearchResult::getContent)
                .orElse("Desculpe, não encontrei uma resposta.");
    }

    private List<Double> fetchEmbeddingVector(String question) {
        EmbeddingRequest request = new EmbeddingRequest(question, embeddingModel);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(embeddingApiKey);

        HttpEntity<EmbeddingRequest> entity = new HttpEntity<>(request, headers);

        logger.info("Enviando para openAI:");

        try {
            ResponseEntity<String> rawResponse = restTemplate.exchange(
                    embeddingApiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            logger.info("Retorno com sucesso da openAI");

            EmbeddingResponse response = objectMapper.readValue(rawResponse.getBody(), EmbeddingResponse.class);
            return response.getFirstEmbedding();
        } catch (Exception e) {
            logger.error("Erro ao chamar a openAI:", e);
            return null;
        }
    }

    private List<SearchResult> queryDatabase(List<Double> vector) {
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
                  "filter": "projectName eq 'tesla_motors'",
                  "vectorQueries": [
                    {
                      "vector": %s,
                      "k": 3,
                      "fields": "embeddings",
                      "kind": "vector"
                    }
                  ]
                }
                """, vectorJson);

        logger.info("Enviando requisição ao banco:");

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> rawResponse = restTemplate.exchange(
                    dbSearchUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            logger.info("Resposta bruta do banco:");
            logger.info(rawResponse.getBody());

            DatabaseSearchResponse typedResponse = objectMapper.readValue(rawResponse.getBody(), DatabaseSearchResponse.class);
            return typedResponse.getValue();
        } catch (Exception e) {
            logger.error("Erro ao consultar o banco:", e);
            return null;
        }
    }
}
