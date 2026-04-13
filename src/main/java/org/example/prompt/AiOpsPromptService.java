package org.example.prompt;

import org.example.prompt.builder.AiOpsExecutorPromptBuilder;
import org.example.prompt.builder.AiOpsPlannerPromptBuilder;
import org.example.prompt.builder.AiOpsSupervisorPromptBuilder;
import org.example.prompt.builder.AiOpsTaskPromptBuilder;
import org.example.prompt.builder.NoPromptContext;
import org.springframework.stereotype.Component;

@Component
public class AiOpsPromptService {

    private final AiOpsPlannerPromptBuilder plannerPromptBuilder;
    private final AiOpsExecutorPromptBuilder executorPromptBuilder;
    private final AiOpsSupervisorPromptBuilder supervisorPromptBuilder;
    private final AiOpsTaskPromptBuilder taskPromptBuilder;

    public AiOpsPromptService(
            AiOpsPlannerPromptBuilder plannerPromptBuilder,
            AiOpsExecutorPromptBuilder executorPromptBuilder,
            AiOpsSupervisorPromptBuilder supervisorPromptBuilder,
            AiOpsTaskPromptBuilder taskPromptBuilder) {
        this.plannerPromptBuilder = plannerPromptBuilder;
        this.executorPromptBuilder = executorPromptBuilder;
        this.supervisorPromptBuilder = supervisorPromptBuilder;
        this.taskPromptBuilder = taskPromptBuilder;
    }

    public String buildPlannerPrompt() {
        return plannerPromptBuilder.build(NoPromptContext.INSTANCE);
    }

    public String buildExecutorPrompt() {
        return executorPromptBuilder.build(NoPromptContext.INSTANCE);
    }

    public String buildSupervisorSystemPrompt() {
        return supervisorPromptBuilder.build(NoPromptContext.INSTANCE);
    }

    public String buildTaskPrompt() {
        return taskPromptBuilder.build(NoPromptContext.INSTANCE);
    }
}
