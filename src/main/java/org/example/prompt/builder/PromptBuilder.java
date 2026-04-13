package org.example.prompt.builder;

public interface PromptBuilder<T> {

    String build(T context);
}
