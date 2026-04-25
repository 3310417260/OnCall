package org.example.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SupervisorAgent;
import org.example.prompt.AiOpsPromptService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentFactory {

    private final AgentToolRegistry agentToolRegistry;
    private final AiOpsPromptService aiOpsPromptService;

    public AgentFactory(AgentToolRegistry agentToolRegistry, AiOpsPromptService aiOpsPromptService) {
        this.agentToolRegistry = agentToolRegistry;
        this.aiOpsPromptService = aiOpsPromptService;
    }

    public ReactAgent createChatAgent(DashScopeChatModel chatModel, String systemPrompt) {
        return ReactAgent.builder()
                .name("intelligent_assistant")
                .model(chatModel)
                .systemPrompt(systemPrompt)
                .methodTools(agentToolRegistry.getChatMethodTools())
                .tools(agentToolRegistry.getChatToolCallbacks())
                .build();
    }

    public ReactAgent createAiOpsPlannerAgent(DashScopeChatModel chatModel) {
        return ReactAgent.builder()
                .name("planner_agent")
                .description("负责拆解告警、规划与再规划步骤")
                .model(chatModel)
                .systemPrompt(aiOpsPromptService.buildPlannerPrompt())
                .methodTools(agentToolRegistry.getPlannerMethodTools())
                .tools(agentToolRegistry.getPlannerToolCallbacks())
                .outputKey("planner_plan")
                .build();
    }

    public ReactAgent createAiOpsExecutorAgent(DashScopeChatModel chatModel) {
        return ReactAgent.builder()
                .name("executor_agent")
                .description("负责执行 Planner 的首个步骤并及时反馈")
                .model(chatModel)
                .systemPrompt(aiOpsPromptService.buildExecutorPrompt())
                .methodTools(agentToolRegistry.getExecutorMethodTools())
                .tools(agentToolRegistry.getExecutorToolCallbacks())
                .outputKey("executor_feedback")
                .build();
    }

    public SupervisorAgent createAiOpsSupervisorAgent(DashScopeChatModel chatModel) {
        ReactAgent plannerAgent = createAiOpsPlannerAgent(chatModel);
        ReactAgent executorAgent = createAiOpsExecutorAgent(chatModel);

        return SupervisorAgent.builder()
                .name("ai_ops_supervisor")
                .description("负责调度 Planner 与 Executor 的多 Agent 控制器")
                .model(chatModel)
                .systemPrompt(aiOpsPromptService.buildSupervisorSystemPrompt())
                .subAgents(List.of(plannerAgent, executorAgent))
                .build();
    }
}
