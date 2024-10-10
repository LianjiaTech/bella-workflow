package com.ke.bella.workflow.api.model;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public enum ModelPropertyKey {
    MODE("mode"),
    CONTEXT_SIZE("context_size"),
    MAX_CHUNKS("max_chunks"),
    FILE_UPLOAD_LIMIT("file_upload_limit"),
    SUPPORTED_FILE_EXTENSIONS("supported_file_extensions"),
    MAX_CHARACTERS_PER_CHUNK("max_characters_per_chunk"),
    DEFAULT_VOICE("default_voice"),
    VOICES("voices"),
    WORD_LIMIT("word_limit"),
    AUDIO_TYPE("audio_type"),
    MAX_WORKERS("max_workers");

    @Getter
    private final String value;

    ModelPropertyKey(String value) {
        this.value = value;
    }

    public static Map<String, Object> chatProperty(int contextSize) {
        Map<String, Object> map = new HashMap<>();
        map.put(MODE.value, "chat");
        map.put(CONTEXT_SIZE.value, contextSize);
        return map;
     }
}
