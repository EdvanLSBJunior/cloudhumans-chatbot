package com.cloudhumans.chatbot.model.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SearchResult {

    @JsonProperty("@search.score")
    private double searchScore;

    private String content;

    private String type;
}
