# OpsPilot AI

> 面向智能问答与 AIOps 排障的 Spring Boot + Agent 实验项目

## 项目简介

`OpsPilot AI` 是一个以运维辅助和知识检索为核心的 AI Agent 项目，目标是把文档知识库、监控告警、日志查询和多轮对话整合到同一个系统中，提供一个可本地运行、可逐步工程化演进的 OnCall 辅助平台。

当前项目包含两条主要能力链路：

- 智能问答：支持多轮会话、工具调用和 SSE 流式输出
- AIOps 分析：支持基于多 Agent 协作的告警诊断和报告生成


## 当前能力

- 基于 DashScope 的聊天与向量化能力
- 基于 Milvus 的文档向量存储与相似检索
- 支持 Markdown / TXT 文档上传并自动切片入库
- 支持 SSE 流式问答接口
- 支持会话上下文管理
- 支持通过 MCP 接入腾讯云日志查询能力
- 支持 Prometheus 告警查询工具
- 支持本地 mock 模式，便于联调和演示


## 技术栈

| 技术 | 说明 |
|------|------|
| Java 17 | 项目目标 Java 版本 |
| Spring Boot 3.2.x | Web 应用基础框架 |
| Spring AI / Spring AI Alibaba | Agent 与模型接入 |
| DashScope | LLM 与 Embedding 能力 |
| Milvus | 向量数据库 |
| MCP | 外部工具能力接入 |


## 项目结构

```text
src/main/java/org/example/
├── controller/
│   ├── ChatController.java
│   ├── AiOpsController.java
│   ├── SessionController.java
│   ├── FileUploadController.java
│   └── MilvusCheckController.java
├── service/
│   ├── ChatService.java
│   ├── AiOpsService.java
│   ├── SessionService.java
│   ├── RagService.java
│   ├── VectorEmbeddingService.java
│   ├── VectorIndexService.java
│   ├── VectorSearchService.java
│   └── DocumentChunkService.java
├── agent/tool/
│   ├── DateTimeTools.java
│   ├── InternalDocsTools.java
│   ├── QueryMetricsTools.java
│   └── QueryLogsTools.java
├── prompt/
│   ├── ChatPromptService.java
│   └── AiOpsPromptService.java
├── dto/
├── response/
└── config/
```


## 核心接口

### 1. 普通聊天

```bash
POST /api/chat
Content-Type: application/json

{
  "Id": "session-123",
  "Question": "什么是向量数据库？"
}
```

### 2. 流式聊天

```bash
POST /api/chat_stream
Content-Type: application/json

{
  "Id": "session-123",
  "Question": "什么是向量数据库？"
}
```

### 3. AIOps 分析

```bash
POST /api/ai_ops
```

### 4. 会话管理

- `POST /api/chat/clear`
- `GET /api/chat/session/{sessionId}`

### 5. 文档上传

- `POST /api/upload`

### 6. Milvus 检查

- `GET /milvus/health`


## 运行依赖

本地运行至少需要：

- JDK 17
- Maven
- Docker Desktop
- DashScope API Key


## 环境变量

项目中的敏感配置建议全部通过环境变量注入。

```bash
export DASHSCOPE_API_KEY=your-api-key
export TENCENT_MCP_SSE_ENDPOINT=/sse/your-private-endpoint
export TENCENT_MCP_BASE_URL=https://mcp-api.tencent-cloud.com
```

说明：

- `DASHSCOPE_API_KEY` 必填
- `TENCENT_MCP_SSE_ENDPOINT` 建议仅保留在本地环境中，不提交到公共仓库
- `TENCENT_MCP_BASE_URL` 可选，不配置时使用默认值


## 快速开始

### 1. 启动向量数据库

```bash
docker compose -f vector-database.yml up -d
```

### 2. 启动服务

```bash
mvn clean install
mvn spring-boot:run
```

### 3. 打开页面

```text
http://localhost:9900
```

### 4. 上传知识库文档

```bash
curl -X POST http://localhost:9900/api/upload \
  -F "file=@document.txt"
```


## 本地联调建议

### Prometheus

如果本地没有真实 Prometheus，建议在 `application.yml` 中先启用 mock：

```yaml
prometheus:
  mock-enabled: true
```

### 日志查询

当前项目支持通过腾讯云 MCP 接入日志工具；如果本地不准备联调真实日志平台，也建议保留 mock / 占位方式进行验证。

### 向量化上传

当前上传接口会同步执行文档切片与向量化，因此在网络较慢时可能出现等待较久或 embedding 超时的情况。这也是后续计划优化的重点之一。


## 当前适合的使用场景

- 本地验证 Agent + RAG 基本链路
- 演示文档知识检索与智能问答
- 验证 AIOps 报告生成流程
- 作为后续重构和工程化改造的基础仓库


## 后续计划

当前项目正在逐步重构，重点方向包括：

- 统一 DTO、响应结构与异常处理
- 拆分控制器与会话管理
- 统一 Prompt 管理
- 优化上传后同步向量化的流程
- 将 mock 与真实外部依赖进一步解耦

详细说明可参考：

- [REFACTOR_PLAN.md](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/REFACTOR_PLAN.md:1)


## 备注

这个项目当前更偏向“可运行的实验性工程骨架”，适合边跑通、边梳理、边重构，而不是一次性追求完整生产化。对外发布时，建议进一步补充：

- 更稳定的配置分层
- 更清晰的 mock / dev / prod 区分
- 更完整的测试
- 更合理的异步任务与状态管理
