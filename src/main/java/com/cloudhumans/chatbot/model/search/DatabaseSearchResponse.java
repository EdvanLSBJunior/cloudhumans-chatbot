package com.cloudhumans.chatbot.model.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabaseSearchResponse {
    private List<SearchResult> value;
}
