package com.cloudhumans.chatbot.model.response;

import com.cloudhumans.chatbot.model.llm.Message;
import com.cloudhumans.chatbot.model.search.SearchResult;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ConversationResponse {
    private List<Message> messages;
    private boolean handoverToHumanNeeded;
    private List<SearchResult> sectionsRetrieved;
}
