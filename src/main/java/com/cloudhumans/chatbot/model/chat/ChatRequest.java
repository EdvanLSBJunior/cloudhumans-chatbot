package com.cloudhumans.chatbot.model.chat;

import com.cloudhumans.chatbot.model.llm.Message;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ChatRequest {
    @NotBlank
    private String projectName;

    @NotEmpty
    private List<Message> messages;
}
