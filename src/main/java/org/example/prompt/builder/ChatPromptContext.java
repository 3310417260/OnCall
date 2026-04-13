package org.example.prompt.builder;

import java.util.List;
import java.util.Map;

public record ChatPromptContext(List<Map<String, String>> history) {

    public ChatPromptContext {
        history = history == null ? List.of() : List.copyOf(history);
    }
}
