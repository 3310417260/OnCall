package org.example.service;

import org.example.dto.ExecutorFeedback;
import org.example.dto.ExecutorStatus;
import org.example.dto.PlannerPlan;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AiOpsVerificationService {

    private static final String REPORT_HEADER = "# 告警分析报告";

    public Optional<String> buildVerifiedReport(PlannerPlan plannerPlan, Optional<ExecutorFeedback> executorFeedback) {
        if (plannerPlan == null || plannerPlan.getReport() == null || plannerPlan.getReport().isBlank()) {
            return Optional.empty();
        }

        String normalizedReport = normalizeHeader(plannerPlan.getReport());
        String verifiedReport = applyEvidenceGuardrail(normalizedReport, executorFeedback);
        return Optional.of(verifiedReport);
    }

    private String normalizeHeader(String report) {
        String trimmed = report.trim();
        if (trimmed.startsWith(REPORT_HEADER)) {
            return trimmed;
        }
        return REPORT_HEADER + "\n\n" + trimmed;
    }

    private String applyEvidenceGuardrail(String report, Optional<ExecutorFeedback> executorFeedback) {
        if (executorFeedback.isEmpty()) {
            return report;
        }

        ExecutorFeedback feedback = executorFeedback.get();
        if (feedback.getStatus() == ExecutorStatus.FAILED) {
            return appendSectionIfMissing(
                    report,
                    "## 执行风险说明",
                    "执行阶段存在失败记录：" + safe(feedback.getError()) + "。以下结论仅基于当前已获取证据，请谨慎使用。"
            );
        }

        if (feedback.getStatus() == ExecutorStatus.NO_DATA) {
            return appendSectionIfMissing(
                    report,
                    "## 数据完整性说明",
                    "执行阶段未获取到有效证据数据。以下结论主要基于告警概览与知识库资料，建议人工复核。"
            );
        }

        if ((feedback.getEvidence() == null || feedback.getEvidence().isEmpty()) && feedback.getStatus() == ExecutorStatus.SUCCESS) {
            return appendSectionIfMissing(
                    report,
                    "## 证据完整性说明",
                    "执行阶段返回 SUCCESS，但未携带结构化证据列表。建议补充日志或指标证据后再确认结论。"
            );
        }

        return report;
    }

    private String appendSectionIfMissing(String report, String sectionTitle, String content) {
        if (report.contains(sectionTitle)) {
            return report;
        }
        return report + "\n\n" + sectionTitle + "\n" + content;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "未提供具体错误详情" : value;
    }
}
