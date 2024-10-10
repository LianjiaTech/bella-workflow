package com.ke.bella.workflow.api.model;

import com.google.common.collect.ImmutableList;
import lombok.Getter;

import java.util.List;

public enum ModelType {
    LLM("llm", "text-generation", ImmutableList.of("/v1/chat/completions")),
    TEXT_EMBEDDING("text-embedding", "embeddings", ImmutableList.of("/v1/embeddings")),
    RERANK("rerank", "reranking", ImmutableList.of()),
    SPEECH2TEXT("speech2text", "speech2text", ImmutableList.of("/v1/audio/transcriptions")),
    MODERATION("moderation", "moderation", ImmutableList.of()),
    TTS("tts", "tts", ImmutableList.of("/v1/audio/speech")),
    TEXT2IMG("text2img", "text2img", ImmutableList.of("/v1/images/generations"));

    @Getter
    private final String value;
    @Getter
    private final String alias;
    @Getter
    private final List<String> endpoints;

    ModelType(String value, String alias, List<String> endpoints) {
        this.value = value;
        this.alias = alias;
        this.endpoints = endpoints;
    }

    public static ModelType of(String modelType) {
        for (ModelType type : ModelType.values()) {
            if (type.getAlias().equals(modelType) || type.getValue().equals(modelType)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid model type: " + modelType);
    }

    public String toAlias() {
        return this.getAlias();
    }
}
