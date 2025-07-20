package com.cloudhumans.chatbot.model.llm;


import lombok.Data;

import java.util.List;

@Data
public class ChatCompletionResponse {
    private List<Choice> choices;

    @Data
    public static class Choice {
        private int index;
        private Message message;
        private Object finish_reason;
    }
}
