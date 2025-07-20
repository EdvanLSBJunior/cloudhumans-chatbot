package com.cloudhumans.chatbot.model.embedding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmbeddingResponse {
    private List<EmbeddingData> data;

    public List<Double> getFirstEmbedding() {
        return data != null && !data.isEmpty() ? data.get(0).getEmbedding() : null;
    }
}
