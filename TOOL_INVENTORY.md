# OpsPilot AI 工具清单

## 1. 文档目的

本文档用于整理当前项目中可被 Agent 使用的工具能力，区分：

- 本地 Method Tools
- 远端 MCP Tools

并记录每类工具的：

- 来源
- 注册方式
- 参数结构
- 主要用途
- 当前限制


## 2. 工具装配总览

当前项目中的工具来源有两类：

1. 本地工具  
由项目中的 Java 方法通过 `@Tool` 暴露。

2. 远端 MCP 工具  
由 Spring AI MCP Client 在运行时连接远端 MCP Server 后动态发现，并封装为 `ToolCallback[]`。

当前装配入口在：

- Chat：`getChatMethodTools()` + `getChatToolCallbacks()`
- Planner：`getPlannerMethodTools()` + `getPlannerToolCallbacks()`
- Executor：`getExecutorMethodTools()` + `getExecutorToolCallbacks()`

对应文件：

- [AgentToolRegistry.java](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/java/org/example/service/AgentToolRegistry.java:1)
- [AgentFactory.java](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/java/org/example/service/AgentFactory.java:1)

可以简化理解成：

```text
Chat Agent
-> methodTools(完整本地工具)
-> tools(完整MCP工具回调)

Planner Agent
-> methodTools(时间 + 知识库 + Prometheus 告警)
-> tools(告警元信息类 MCP 工具)

Executor Agent
-> methodTools(时间 + 日志)
-> tools(日志/指标取证类 MCP 工具)
```

### 2.1 角色化工具分配

当前 AIOps workflow 中的工具边界已经按角色拆分：

| Agent | 本地工具 | MCP 工具方向 | 设计目的 |
|------|------|------|------|
| Chat | 时间、知识库、Prometheus、日志 | 全量远端工具 | 保持通用问答能力 |
| Planner | 时间、知识库、Prometheus | 告警概览、告警元信息、辅助转换 | 定方向、做计划、决定是否 replan |
| Executor | 时间、日志 | 日志搜索、日志上下文、指标查询、辅助转换 | 执行当前步骤并收集证据 |


## 3. 结构化摘要

```json
{
  "local_method_tools": 5,
  "remote_mcp_tools": 19,
  "local_sources": [
    "DateTimeTools",
    "InternalDocsTools",
    "QueryMetricsTools",
    "QueryLogsTools"
  ],
  "mcp_source": "Tencent CLS MCP Server via Spring AI MCP Client",
  "mcp_tool_snapshot_source": "server.log lines 1055-1074"
}
```


## 4. 本地 Method Tools

### 4.1 清单表

| 工具名 | 来源类 | 参数 | 返回 | 主要用途 | 备注 |
|------|------|------|------|------|------|
| `getCurrentDateTime` | `DateTimeTools` | 无 | 当前时间字符串 | 获取当前时间 | 轻量本地工具 |
| `queryInternalDocs` | `InternalDocsTools` | `query` | JSON 检索结果 | 查询内部知识库 / RAG | 依赖向量检索 |
| `queryPrometheusAlerts` | `QueryMetricsTools` | 无 | JSON 告警结果 | 查询当前活动告警 | 支持 mock / real |
| `getAvailableLogTopics` | `QueryLogsTools` | 无 | JSON 主题列表 | 获取可用日志主题 | 当前主要服务于日志查询前置引导 |
| `queryLogs` | `QueryLogsTools` | `region`, `logTopic`, `query`, `limit` | JSON 日志结果 | 查询 CLS / 模拟日志 | 当前真实 CLS 逻辑仍为占位 |


### 4.2 详细说明

#### `getCurrentDateTime`

- 来源文件：
  [DateTimeTools.java](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/java/org/example/agent/tool/DateTimeTools.java:1)
- 参数：
  - 无
- 返回：
  - 用户时区下的当前日期时间字符串
- 适用场景：
  - 时间问题
  - 生成时间上下文

#### `queryInternalDocs`

- 来源文件：
  [InternalDocsTools.java](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/java/org/example/agent/tool/InternalDocsTools.java:1)
- 参数：
  - `query`: 搜索查询
- 返回：
  - JSON 格式的相似文档列表，包含内容、分数和 metadata
- 底层依赖：
  - `VectorSearchService`
  - `VectorEmbeddingService`
  - `Milvus`
- 适用场景：
  - 查询 SOP
  - 查询内部技术文档
  - 查询流程、最佳实践、处理手册

#### `queryPrometheusAlerts`

- 来源文件：
  [QueryMetricsTools.java](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/java/org/example/agent/tool/QueryMetricsTools.java:1)
- 参数：
  - 无
- 返回：
  - JSON 格式的活动告警列表
- 支持模式：
  - `mock-enabled=true` 时返回模拟告警
  - `mock-enabled=false` 时调用 `GET /api/v1/alerts`
- 适用场景：
  - 告警排查
  - AIOps 分析
  - 查看当前 Prometheus firing alerts

#### `getAvailableLogTopics`

- 来源文件：
  [QueryLogsTools.java](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/java/org/example/agent/tool/QueryLogsTools.java:1)
- 参数：
  - 无
- 返回：
  - JSON 格式的日志主题列表
- 预置主题：
  - `system-metrics`
  - `application-logs`
  - `database-slow-query`
  - `system-events`
- 适用场景：
  - 在执行日志查询前确定 topic 和 query 模式

#### `queryLogs`

- 来源文件：
  [QueryLogsTools.java](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/java/org/example/agent/tool/QueryLogsTools.java:1)
- 参数：
  - `region`
  - `logTopic`
  - `query`
  - `limit`
- 返回：
  - JSON 格式的日志结果
- 当前行为：
  - `mock-enabled=true` 时返回与告警关联的模拟日志
  - `mock-enabled=false` 时当前真实查询仍返回占位错误
- 适用场景：
  - 本地联调
  - 模拟告警排查


## 5. 远端 MCP Tools

### 5.1 来源说明

远端工具不是在本地代码中逐个定义的，而是：

- 通过 Spring AI MCP Client 连接远端腾讯云 MCP Server
- 在运行时动态发现
- 由 `ToolCallbackProvider` 封装后注入到 Agent

配置文件：

- [application.yml](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/resources/application.yml:39)

当前远端 Server 标识（来自日志）：

- `Implementation[name=cls-mcp-server, version=0.2.8]`

日志证据：

- [server.log](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/server.log:57)


### 5.2 当前发现到的 MCP Tool 列表

以下工具名来自运行日志快照：

- [server.log](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/server.log:1055)

```json
[
  "ConvertTimeStringToTimestamp",
  "ConvertTimestampToTimeString",
  "DescribeAlarmNotices",
  "DescribeAlarmShields",
  "DescribeAlarms",
  "DescribeAlertRecordHistory",
  "DescribeIndex",
  "DescribeLogContext",
  "DescribeLogHistogram",
  "DescribeNoticeContents",
  "DescribeWebCallbacks",
  "GetAlarmDetail",
  "GetAlarmLog",
  "GetRegionCodeByName",
  "GetTopicInfoByName",
  "QueryMetric",
  "QueryRangeMetric",
  "SearchLog",
  "TextToSearchLogQuery"
]
```


### 5.3 分类说明

| MCP 工具名 | 推测分类 | 主要用途 | 备注 |
|------|------|------|------|
| `ConvertTimeStringToTimestamp` | 时间转换 | 时间字符串转时间戳 | 辅助查询参数处理 |
| `ConvertTimestampToTimeString` | 时间转换 | 时间戳转可读时间 | 辅助结果展示 |
| `DescribeAlarmNotices` | 告警管理 | 查询告警通知配置 | 告警侧辅助工具 |
| `DescribeAlarmShields` | 告警管理 | 查询告警屏蔽信息 | 告警治理辅助 |
| `DescribeAlarms` | 告警查询 | 获取告警列表 | 告警入口工具 |
| `DescribeAlertRecordHistory` | 告警历史 | 查询告警记录历史 | 历史排障辅助 |
| `DescribeIndex` | 日志索引 | 查看日志索引结构 | 日志查询前置工具 |
| `DescribeLogContext` | 日志上下文 | 获取日志上下文 | 定位上下文证据 |
| `DescribeLogHistogram` | 日志分析 | 查询日志时间分布 | 日志时序分析 |
| `DescribeNoticeContents` | 通知内容 | 查看通知内容 | 告警通知辅助 |
| `DescribeWebCallbacks` | 回调配置 | 查看 Webhook / 回调配置 | 告警联动辅助 |
| `GetAlarmDetail` | 告警详情 | 查询具体告警详情 | 核心告警工具 |
| `GetAlarmLog` | 告警日志 | 查看告警相关日志 | 告警排查辅助 |
| `GetRegionCodeByName` | 地域辅助 | 地域名称转 code | 参数标准化 |
| `GetTopicInfoByName` | 日志主题辅助 | 按主题名查询 topic 信息 | 日志查询前置辅助 |
| `QueryMetric` | 指标查询 | 查询当前指标 | 监控查询工具 |
| `QueryRangeMetric` | 指标查询 | 查询时间范围指标 | 趋势分析工具 |
| `SearchLog` | 日志查询 | 按条件搜索日志 | 核心日志工具 |
| `TextToSearchLogQuery` | 查询转换 | 自然语言转日志查询语句 | 查询构造辅助 |

说明：

- 以上“推测分类”和“主要用途”是根据工具名和当前业务上下文归纳出来的
- 当前仓库中没有保存这些远端工具的完整 schema，因此参数级细节需要以远端 MCP Server 实时暴露结果为准


## 6. 当前工具体系的关系

### 6.1 本地工具与 MCP 工具的关系

当前 Agent 运行时拿到的是一个混合工具池：

```text
methodTools
  -> 本地 @Tool
tools
  -> MCP ToolCallbacks
```

即：

- 本地工具负责项目内原生能力
- MCP 工具负责接入远端腾讯云日志 / 告警 / 指标等能力


### 6.2 当前最关键的两条工具链

#### 普通问答

- 本地知识库检索：`queryInternalDocs`
- 本地告警查询：`queryPrometheusAlerts`
- 远端日志 / 指标：通过 MCP tools 动态调用

#### AIOps 分析

- Planner / Executor 可以同时使用：
  - 本地知识库
  - 本地 Prometheus tool
  - 远端 MCP 日志 / 指标 / 告警工具


## 7. 当前限制

### 7.1 远端 MCP Tools 的 schema 不在仓库中

这意味着：

- 当前代码仓里只能确定“发现到了哪些工具名”
- 但不能仅靠仓库静态文件还原每个远端工具的完整参数 schema

### 7.2 本地 `queryLogs` 与远端 MCP 日志工具存在语义重叠

当前项目中：

- `QueryLogsTools` 提供本地 mock / 占位日志能力
- 远端 MCP Server 同时也提供 `SearchLog` 等日志工具

这说明当前工具体系还在演进中，后续更适合继续明确：

- 哪些能力保留本地 tool
- 哪些能力完全交给 MCP


## 8. 总结

当前项目里的工具能力可以概括为：

- 本地 Method Tools：负责时间、RAG、Prometheus、Mock 日志等能力
- 远端 MCP Tools：负责接入腾讯云日志、告警、指标等平台能力

如果用一句话总结：

> 这个项目当前使用的是“本地工具 + Spring AI MCP 动态发现工具”的混合模式，本地工具解决项目内原生能力，远端 MCP 工具补齐日志、告警和监控侧的外部平台能力。
