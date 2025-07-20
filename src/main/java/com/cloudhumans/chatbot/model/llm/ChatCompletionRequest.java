package com.cloudhumans.chatbot.model.llm;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ChatCompletionRequest {
    private String model;
    private List<Message> messages;
}
