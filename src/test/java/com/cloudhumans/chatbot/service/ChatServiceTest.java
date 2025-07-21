package com.cloudhumans.chatbot.service;

import com.cloudhumans.chatbot.TestUtils;
import com.cloudhumans.chatbot.model.embedding.EmbeddingResponse;
import com.cloudhumans.chatbot.model.embedding.EmbeddingData;
import com.cloudhumans.chatbot.model.llm.ChatCompletionResponse;
import com.cloudhumans.chatbot.model.llm.Message;
import com.cloudhumans.chatbot.model.response.ConversationResponse;
import com.cloudhumans.chatbot.model.search.DatabaseSearchResponse;
import com.cloudhumans.chatbot.model.search.SearchResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @InjectMocks
    private ChatService chatService;

    @Mock
    private RestTemplate restTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        chatService = new ChatService(restTemplate, objectMapper);

        TestUtils.setField(chatService, "embeddingApiUrl", "http://fake-embedding-api");
        TestUtils.setField(chatService, "embeddingApiKey", "abc123");
        TestUtils.setField(chatService, "embeddingModel", "text-embedding-ada-002");
        TestUtils.setField(chatService, "dbSearchUrl", "http://fake-vector-db");
        TestUtils.setField(chatService, "dbApiKey", "dbkey123");
        TestUtils.setField(chatService, "openAiChatUrl", "http://fake-openai");
        TestUtils.setField(chatService, "openAiApiKey", "openkey123");
        TestUtils.setField(chatService, "openAiModel", "gpt-4");
    }

    @Test
    void testGetAnswerValidFlow() throws Exception {
        String userMessage = "What should I do if my car catches fire?";
        String projectName = "TeslaProject";

        EmbeddingData embeddingData = new EmbeddingData();
        embeddingData.setEmbedding(List.of(0.1, 0.2, 0.3));
        EmbeddingResponse embeddingResponse = new EmbeddingResponse();
        embeddingResponse.setData(List.of(embeddingData));

        String embeddingJson = objectMapper.writeValueAsString(embeddingResponse);
        when(restTemplate.exchange(
                eq("http://fake-embedding-api"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok(embeddingJson));

        SearchResult result = new SearchResult();
        result.setContent("If your car is on fire, leave immediately.");
        result.setType("N2");

        DatabaseSearchResponse dbResponse = new DatabaseSearchResponse();
        dbResponse.setValue(List.of(result));

        String dbJson = objectMapper.writeValueAsString(dbResponse);
        when(restTemplate.exchange(
                eq("http://fake-vector-db"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok(dbJson));

        ChatCompletionResponse.Choice choice = new ChatCompletionResponse.Choice();
        choice.setMessage(new Message("assistant", "If your car is on fire, exit immediately."));

        ChatCompletionResponse chatResponse = new ChatCompletionResponse();
        chatResponse.setChoices(List.of(choice));

        when(restTemplate.exchange(
                eq("http://fake-openai"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(ChatCompletionResponse.class)
        )).thenReturn(ResponseEntity.ok(chatResponse));

        ConversationResponse response = chatService.getAnswer(projectName, userMessage);

        assertThat(response).isNotNull();
        assertThat(response.getMessages()).hasSize(2);
        assertThat(response.getMessages().get(1).getContent()).contains("exit immediately");
        assertThat(response.isHandoverToHumanNeeded()).isTrue();
    }

    @Test
    void testEmbeddingFailure() {
        String userMessage = "My car is on fire!";
        String projectName = "TeslaProject";

        when(restTemplate.exchange(
                eq("http://fake-embedding-api"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(null);

        ConversationResponse response = chatService.getAnswer(projectName, userMessage);

        assertThat(response).isNotNull();
        assertThat(response.getMessages()).hasSize(2);
        assertThat(response.getMessages().get(1).getContent()).contains("Erro ao gerar embedding");
        assertThat(response.getResults()).isEmpty();
        assertThat(response.isHandoverToHumanNeeded()).isTrue();
    }

    @Test
    void testNoResultsFromDatabase() throws Exception {
        String userMessage = "Battery exploded!";
        String projectName = "TeslaProject";

        String embeddingJson = """
                {
                  "data": [{ "embedding": [0.1, 0.2, 0.3] }]
                }
                """;
        when(restTemplate.exchange(
                eq("http://fake-embedding-api"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok(embeddingJson));

        String dbJson = """
                { "value": [] }
                """;
        when(restTemplate.exchange(
                eq("http://fake-vector-db"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok(dbJson));

        ConversationResponse response = chatService.getAnswer(projectName, userMessage);

        assertThat(response.getMessages()).hasSize(2);
        assertThat(response.getMessages().get(1).getContent()).contains("não encontrei uma resposta");
        assertThat(response.getResults()).isEmpty();
        assertThat(response.isHandoverToHumanNeeded()).isTrue();
    }

    @Test
    void testGpt4Failure() throws Exception {
        String userMessage = "Is my battery waterproof?";
        String projectName = "TeslaProject";

        String embeddingJson = """
                {
                  "data": [{ "embedding": [0.1, 0.2, 0.3] }]
                }
                """;
        when(restTemplate.exchange(
                eq("http://fake-embedding-api"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok(embeddingJson));

        String dbJson = """
                {
                  "value": [{
                    "content": "Tesla batteries are not waterproof.",
                    "type": "N2"
                  }]
                }
                """;
        when(restTemplate.exchange(
                eq("http://fake-vector-db"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok(dbJson));

        when(restTemplate.exchange(
                eq("http://fake-openai"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(ChatCompletionResponse.class)
        )).thenReturn(null);

        ConversationResponse response = chatService.getAnswer(projectName, userMessage);

        assertThat(response.getMessages().get(1).getContent()).contains("Erro ao gerar resposta via LLM");
    }

    @Test
    void testHasN2False() throws Exception {
        String userMessage = "Is it solar-powered?";
        String projectName = "TeslaProject";

        String embeddingJson = """
                {
                  "data": [{ "embedding": [0.1, 0.2, 0.3] }]
                }
                """;
        when(restTemplate.exchange(
                eq("http://fake-embedding-api"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok(embeddingJson));

        String dbJson = """
                {
                  "value": [{
                    "content": "No solar charging available.",
                    "type": "N1"
                  }]
                }
                """;
        when(restTemplate.exchange(
                eq("http://fake-vector-db"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok(dbJson));

        ChatCompletionResponse.Choice choice = new ChatCompletionResponse.Choice();
        choice.setMessage(new Message("assistant", "No solar charging."));
        ChatCompletionResponse chatResponse = new ChatCompletionResponse();
        chatResponse.setChoices(List.of(choice));

        when(restTemplate.exchange(
                eq("http://fake-openai"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(ChatCompletionResponse.class)
        )).thenReturn(ResponseEntity.ok(chatResponse));

        ConversationResponse response = chatService.getAnswer(projectName, userMessage);

        assertThat(response.isHandoverToHumanNeeded()).isFalse();
    }
}
