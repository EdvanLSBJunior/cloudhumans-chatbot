package com.cloudhumans.chatbot.model.embedding;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmbeddingData {
    private List<Double> embedding;
}
