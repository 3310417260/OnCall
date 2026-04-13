package org.example.prompt;

import org.example.prompt.builder.ChatPromptBuilder;
import org.example.prompt.builder.ChatPromptContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ChatPromptService {

    private final ChatPromptBuilder chatPromptBuilder;

    public ChatPromptService(ChatPromptBuilder chatPromptBuilder) {
        this.chatPromptBuilder = chatPromptBuilder;
    }

    public String buildSystemPrompt(List<Map<String, String>> history) {
        return chatPromptBuilder.build(new ChatPromptContext(history));
    }
}
