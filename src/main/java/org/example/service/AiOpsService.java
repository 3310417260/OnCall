package org.example.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SupervisorAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.example.dto.ExecutorFeedback;
import org.example.dto.PlannerDecision;
import org.example.dto.PlannerPlan;
import org.example.prompt.AiOpsPromptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * AI Ops 智能运维服务
 * 负责多 Agent 协作的告警分析流程
 */
@Service
public class AiOpsService {

    private static final Logger logger = LoggerFactory.getLogger(AiOpsService.class);

    private final AgentFactory agentFactory;
    private final AiOpsPromptService aiOpsPromptService;
    private final AiOpsStateService aiOpsStateService;
    private final AiOpsVerificationService aiOpsVerificationService;

    public AiOpsService(
            AgentFactory agentFactory,
            AiOpsPromptService aiOpsPromptService,
            AiOpsStateService aiOpsStateService,
            AiOpsVerificationService aiOpsVerificationService) {
        this.agentFactory = agentFactory;
        this.aiOpsPromptService = aiOpsPromptService;
        this.aiOpsStateService = aiOpsStateService;
        this.aiOpsVerificationService = aiOpsVerificationService;
    }

    /**
     * 执行 AI Ops 告警分析流程
     *
     * @param chatModel      大模型实例
     * @return 分析结果状态
     * @throws GraphRunnerException 如果 Agent 执行失败
     */
    public Optional<OverAllState> executeAiOpsAnalysis(DashScopeChatModel chatModel) throws GraphRunnerException {
        logger.info("开始执行 AI Ops 多 Agent 协作流程");

        SupervisorAgent supervisorAgent = agentFactory.createAiOpsSupervisorAgent(chatModel);

        String taskPrompt = aiOpsPromptService.buildTaskPrompt();

        logger.info("调用 Supervisor Agent 开始编排...");
        return supervisorAgent.invoke(taskPrompt);
    }

    /**
     * 从执行结果中提取最终报告文本
     *
     * @param state 执行状态
     * @return 报告文本（如果存在）
     */
    public Optional<String> extractFinalReport(OverAllState state) {
        logger.info("开始提取最终报告...");

        Optional<PlannerPlan> plannerPlan = extractPlannerPlan(state);
        if (plannerPlan.isPresent()
                && plannerPlan.get().getDecision() == PlannerDecision.FINISH
                && plannerPlan.get().getReport() != null
                && !plannerPlan.get().getReport().isBlank()) {
            Optional<String> verifiedReport = aiOpsVerificationService.buildVerifiedReport(
                    plannerPlan.get(),
                    extractExecutorFeedback(state)
            );
            if (verifiedReport.isPresent()) {
                logger.info("成功提取并校验结构化 Planner 最终报告，长度: {}", verifiedReport.get().length());
                return verifiedReport;
            }
        }

        Optional<AssistantMessage> plannerFinalOutput = getAssistantMessage(state, "planner_plan");
        if (plannerFinalOutput.isPresent()) {
            String rawText = plannerFinalOutput.get().getText();
            logger.warn("Planner 最终输出未成功结构化解析，回退为原始文本");
            return Optional.ofNullable(rawText).filter(text -> !text.isBlank());
        }

        logger.warn("未能提取到 Planner 最终报告");
        return Optional.empty();
    }

    public Optional<PlannerPlan> extractPlannerPlan(OverAllState state) {
        return getAssistantMessage(state, "planner_plan")
                .flatMap(message -> parsePlannerPlan(message.getText()));
    }

    public Optional<ExecutorFeedback> extractExecutorFeedback(OverAllState state) {
        return getAssistantMessage(state, "executor_feedback")
                .flatMap(message -> parseExecutorFeedback(message.getText()));
    }

    private Optional<PlannerPlan> parsePlannerPlan(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(aiOpsStateService.parsePlannerPlan(rawText));
        } catch (Exception e) {
            logger.warn("解析 PlannerPlan 失败: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<ExecutorFeedback> parseExecutorFeedback(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(aiOpsStateService.parseExecutorFeedback(rawText));
        } catch (Exception e) {
            logger.warn("解析 ExecutorFeedback 失败: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<AssistantMessage> getAssistantMessage(OverAllState state, String key) {
        return state.value(key)
                .filter(AssistantMessage.class::isInstance)
                .map(AssistantMessage.class::cast);
    }
}
