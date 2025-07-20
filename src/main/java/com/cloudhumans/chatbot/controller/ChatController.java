package com.cloudhumans.chatbot.controller;

import com.cloudhumans.chatbot.model.chat.ChatRequest;
import com.cloudhumans.chatbot.model.chat.ChatResponse;
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
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        String answer = chatService.getAnswer(request.getQuestion());
        return ResponseEntity.ok(new ChatResponse(answer));
    }
}
