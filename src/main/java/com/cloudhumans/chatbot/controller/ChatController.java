package com.cloudhumans.chatbot.controller;

import com.cloudhumans.chatbot.model.chat.ChatRequest;
import com.cloudhumans.chatbot.model.response.ConversationResponse;
import com.cloudhumans.chatbot.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ConversationResponse> chat(@Valid @RequestBody ChatRequest request) {
        String userMessage = request.getMessages().get(request.getMessages().size() - 1).getContent();
        ConversationResponse response = chatService.getAnswer(request.getProjectName(), userMessage);
        return ResponseEntity.ok(response);
    }
}
