package org.example.service;

import org.example.agent.tool.DateTimeTools;
import org.example.agent.tool.InternalDocsTools;
import org.example.agent.tool.QueryLogsTools;
import org.example.agent.tool.QueryMetricsTools;
import org.slf4j.Logger;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.Optional;

@Service
public class AgentToolRegistry {

    private static final Set<String> PLANNER_MCP_TOOL_NAMES = Set.of(
            "ConvertTimeStringToTimestamp",
            "ConvertTimestampToTimeString",
            "DescribeAlarmNotices",
            "DescribeAlarmShields",
            "DescribeAlarms",
            "DescribeAlertRecordHistory",
            "DescribeNoticeContents",
            "DescribeWebCallbacks",
            "GetAlarmDetail",
            "GetRegionCodeByName",
            "GetTopicInfoByName"
    );

    private static final Set<String> EXECUTOR_MCP_TOOL_NAMES = Set.of(
            "ConvertTimeStringToTimestamp",
            "ConvertTimestampToTimeString",
            "DescribeIndex",
            "DescribeLogContext",
            "DescribeLogHistogram",
            "GetAlarmLog",
            "GetRegionCodeByName",
            "GetTopicInfoByName",
            "QueryMetric",
            "QueryRangeMetric",
            "SearchLog",
            "TextToSearchLogQuery"
    );

    private final DateTimeTools dateTimeTools;
    private final InternalDocsTools internalDocsTools;
    private final QueryMetricsTools queryMetricsTools;
    private final QueryLogsTools queryLogsTools;
    private final ToolCallbackProvider toolCallbackProvider;

    public AgentToolRegistry(
            DateTimeTools dateTimeTools,
            InternalDocsTools internalDocsTools,
            QueryMetricsTools queryMetricsTools,
            ToolCallbackProvider toolCallbackProvider,
            Optional<QueryLogsTools> queryLogsTools) {
        this.dateTimeTools = dateTimeTools;
        this.internalDocsTools = internalDocsTools;
        this.queryMetricsTools = queryMetricsTools;
        this.toolCallbackProvider = toolCallbackProvider;
        this.queryLogsTools = queryLogsTools.orElse(null);
    }

    public Object[] getChatMethodTools() {
        if (queryLogsTools != null) {
            return new Object[]{dateTimeTools, internalDocsTools, queryMetricsTools, queryLogsTools};
        }
        return new Object[]{dateTimeTools, internalDocsTools, queryMetricsTools};
    }

    public Object[] getPlannerMethodTools() {
        return new Object[]{dateTimeTools, internalDocsTools, queryMetricsTools};
    }

    public Object[] getExecutorMethodTools() {
        if (queryLogsTools != null) {
            return new Object[]{dateTimeTools, queryLogsTools};
        }
        return new Object[]{dateTimeTools};
    }

    public ToolCallback[] getChatToolCallbacks() {
        return toolCallbackProvider.getToolCallbacks();
    }

    public ToolCallback[] getPlannerToolCallbacks() {
        return filterToolCallbacks(PLANNER_MCP_TOOL_NAMES);
    }

    public ToolCallback[] getExecutorToolCallbacks() {
        return filterToolCallbacks(EXECUTOR_MCP_TOOL_NAMES);
    }

    private ToolCallback[] filterToolCallbacks(Set<String> allowedToolNames) {
        return Arrays.stream(toolCallbackProvider.getToolCallbacks())
                .filter(toolCallback -> allowedToolNames.contains(toolCallback.getToolDefinition().name()))
                .toArray(ToolCallback[]::new);
    }

    public void logAvailableTools(Logger logger) {
        ToolCallback[] toolCallbacks = getChatToolCallbacks();
        logger.info("可用工具列表:");
        for (ToolCallback toolCallback : toolCallbacks) {
            logger.info(">>> {}", toolCallback.getToolDefinition().name());
        }
    }
}
