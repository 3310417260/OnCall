package org.example.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.ExecutorFeedback;
import org.example.dto.ExecutorStatus;
import org.example.dto.PlannerDecision;
import org.example.dto.PlannerPlan;
import org.example.exception.ApiException;
import org.springframework.stereotype.Service;

@Service
public class AiOpsStateService {

    private final ObjectMapper objectMapper;

    public AiOpsStateService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PlannerPlan parsePlannerPlan(String rawText) {
        PlannerPlan plan = readJson(rawText, PlannerPlan.class, "PlannerPlan");
        validatePlannerPlan(plan);
        return plan;
    }

    public ExecutorFeedback parseExecutorFeedback(String rawText) {
        ExecutorFeedback feedback = readJson(rawText, ExecutorFeedback.class, "ExecutorFeedback");
        validateExecutorFeedback(feedback);
        return feedback;
    }

    public String toJsonSchemaExample(PlannerPlan plannerPlan) {
        return writeValue(plannerPlan, "PlannerPlan");
    }

    public String toJsonSchemaExample(ExecutorFeedback executorFeedback) {
        return writeValue(executorFeedback, "ExecutorFeedback");
    }

    private <T> T readJson(String rawText, Class<T> type, String label) {
        if (rawText == null || rawText.isBlank()) {
            throw ApiException.badRequest(label + " 为空，无法解析");
        }

        String jsonText = normalizeJson(rawText);
        try {
            return objectMapper.readValue(jsonText, type);
        } catch (JsonProcessingException e) {
            throw ApiException.badRequest(label + " 不是合法 JSON: " + e.getOriginalMessage());
        }
    }

    private String writeValue(Object value, String label) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("生成 " + label + " 示例失败", e);
        }
    }

    private String normalizeJson(String rawText) {
        String text = rawText.trim();
        if (text.startsWith("```")) {
            int firstNewLine = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstNewLine > 0 && lastFence > firstNewLine) {
                text = text.substring(firstNewLine + 1, lastFence).trim();
            }
        }
        return text;
    }

    private void validatePlannerPlan(PlannerPlan plan) {
        if (plan.getDecision() == null) {
            throw ApiException.badRequest("PlannerPlan 缺少 decision");
        }

        if (plan.getDecision() == PlannerDecision.EXECUTE) {
            requireText(plan.getStep(), "PlannerPlan.step");
            requireText(plan.getTool(), "PlannerPlan.tool");
        }

        if (plan.getDecision() == PlannerDecision.PLAN) {
            requireText(plan.getReason(), "PlannerPlan.reason");
        }

        if (plan.getDecision() == PlannerDecision.FINISH) {
            requireText(plan.getReport(), "PlannerPlan.report");
        }
    }

    private void validateExecutorFeedback(ExecutorFeedback feedback) {
        if (feedback.getStatus() == null) {
            throw ApiException.badRequest("ExecutorFeedback 缺少 status");
        }

        requireText(feedback.getSummary(), "ExecutorFeedback.summary");

        if (feedback.getStatus() == ExecutorStatus.FAILED) {
            requireText(feedback.getError(), "ExecutorFeedback.error");
        }

        if (feedback.getEvidence() == null) {
            feedback.setEvidence(new java.util.ArrayList<>());
        }
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest(fieldName + " 不能为空");
        }
    }
}
