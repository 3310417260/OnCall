# SuperBizAgent 重构说明

## 1. 文档目的

本文档用于说明 `SuperBizAgent` 项目的重构方向、分阶段实施计划、环境要求和待补充配置项。

当前目标不是一次性重写项目，而是在尽量不破坏现有功能的前提下，逐步完成以下工作：

- 降低控制器和服务之间的耦合
- 清理重复 DTO 和响应结构
- 明确 Agent、RAG、向量检索、基础设施接入的边界
- 为后续接入真实 API 和生产化部署做好准备
- 为测试、配置管理和运维扩展留出清晰结构


## 2. 当前项目现状

项目当前是一个基于 Spring Boot 3 + Spring AI Alibaba + Milvus + DashScope 的单体应用，包含三条主要能力链路：

- 通用智能问答
- AIOps 自动告警分析
- 文档上传、向量化与知识库检索

当前实现能跑通主要演示链路，但整体仍处于 PoC / Demo 到初步工程化之间，主要问题集中在结构和边界设计上，而不是功能完全缺失。


## 3. 当前主要问题

### 3.1 控制器职责过重

`ChatController` 同时承担了以下职责：

- 普通聊天接口
- 流式聊天接口
- AIOps 接口
- 会话管理
- 请求/响应 DTO 定义
- SSE 输出结构定义

问题：

- 文件体积过大，理解和维护成本高
- 业务逻辑与接口层混杂
- 会话逻辑、SSE 逻辑难以复用


### 3.2 DTO 和统一响应结构重复

当前存在多个分散定义：

- `ChatController` 内部定义 `ApiResponse`
- `FileUploadController` 内部再次定义 `ApiResponse`
- `ClearRequest`、`SessionInfoResponse` 等定义在控制器内部

问题：

- 数据结构分散，不利于复用
- 返回格式约束不统一
- 后续加测试和接口文档会很痛苦


### 3.3 服务边界不清晰

当前的 `ChatService`、`AiOpsService`、`RagService`、`Vector*Service` 虽然已经拆分，但仍存在以下问题：

- Agent 创建逻辑分散
- Prompt 拼接逻辑和业务流程耦合
- 工具装配逻辑重复
- RAG 服务存在，但未成为主要对外链路

问题：

- 后续改一个 Agent 配置可能需要改多处
- 真实日志接入、Mock 模式、MCP 模式边界不清楚


### 3.4 工具层与基础设施耦合

例如：

- `QueryMetricsTools` 同时处理 mock 数据和真实 Prometheus 调用
- `QueryLogsTools` 同时承担工具定义、主题说明、mock 数据生成、未来真实 CLS 接入占位
- `VectorEmbeddingService` 直接依赖 DashScope SDK 细节
- `VectorIndexService` 直接操作 Milvus 插入和删除

问题：

- mock 与真实实现混在一起
- 难以替换底层服务
- 单元测试难做


### 3.5 配置与运行方式仍偏示例化

当前存在以下工程化不足：

- 前端 `app.js` 中写死 `http://localhost:9900/api`
- 缺少环境分层配置（dev/test/prod）
- 缺少统一配置校验机制
- 缺少启动前依赖检查和更清晰的本地运行说明


### 3.6 可测试性不足

当前仓库几乎没有自动化测试，后续重构时风险较高。

问题：

- 无法快速验证控制器返回是否兼容前端
- 无法验证会话管理、分片逻辑、向量检索参数是否被破坏
- 重构成本会随着代码增长快速上升


## 4. 重构目标

本次重构的目标不是“换技术栈”，而是“把现有代码整理成可持续演进的工程结构”。

目标如下：

1. 保持已有核心功能可继续使用
2. 提高模块边界清晰度
3. 为真实 API 接入提供稳定适配层
4. 统一 DTO、响应结构、异常处理和配置方式
5. 提升 macOS 本地开发体验
6. 为后续逐步补测试和部署做准备


## 5. 重构原则

### 5.1 先整理结构，再逐步替换实现

优先做低风险结构重构：

- 抽 DTO
- 拆控制器
- 抽会话服务
- 抽 Agent 工厂

避免一开始同时大改：

- 提示词
- 工具行为
- 前端交互
- API 返回结构


### 5.2 每一阶段都应尽量可运行

每完成一个阶段，应尽量保证：

- 接口仍可启动
- 主要端到端链路仍可使用
- 不需要等待全部改完才能验证


### 5.3 Mock 与真实接入必须分层

后续要将以下模式显式分开：

- Mock 实现
- MCP / 外部平台实现
- 默认装配策略

不要继续在单一类中通过大量 `if (mockEnabled)` 混合处理。


## 6. 建议的目标结构

建议逐步演进为如下目录组织：

```text
src/main/java/org/example/
├── api
│   ├── controller
│   ├── dto
│   ├── response
│   └── exception
├── application
│   ├── chat
│   ├── aiops
│   ├── session
│   └── rag
├── domain
│   ├── document
│   ├── vector
│   └── alert
├── infrastructure
│   ├── ai
│   ├── milvus
│   ├── prometheus
│   ├── logs
│   ├── storage
│   └── config
└── web
    └── static
```

说明：

- `api` 放 HTTP 层
- `application` 放业务流程编排
- `domain` 放核心模型和纯逻辑
- `infrastructure` 放外部系统接入

现阶段不需要一步到位迁移全部代码，但后续重构应尽量往这个方向靠拢。


## 7. 分阶段实施计划

### 阶段 1：接口层瘦身与统一模型

目标：

- 拆分过大的控制器
- 统一响应结构
- 提取 DTO 与会话模型

计划动作：

- 拆分 `ChatController`
  - `ChatController`
  - `AiOpsController`
  - `SessionController`
- 提取统一响应对象
  - `ApiResponse<T>`
  - `SseMessage`
- 提取请求/响应 DTO
  - `ChatRequest`
  - `ClearSessionRequest`
  - `SessionInfoResponse`
  - `FileUploadResponse`
- 增加全局异常处理
  - `GlobalExceptionHandler`
- 统一返回格式和错误处理策略

阶段结果：

- 控制器更聚焦
- DTO 复用更清晰
- 前后端接口稳定性更好


### 阶段 2：会话与流式输出解耦

目标：

- 将会话和流式输出从控制器中抽离

计划动作：

- 提取 `SessionService`
  - 获取/创建会话
  - 添加历史消息
  - 清空历史消息
  - 查询会话信息
- 提取 `SessionWindowPolicy`
  - 统一消息窗口策略
- 提取 `SseEmitterService` 或 `StreamResponseService`
  - 统一发送 `content/error/done`
  - 降低控制器对 SSE 细节的依赖

阶段结果：

- 控制器不再直接维护锁和会话列表
- SSE 处理更容易测试和复用


### 阶段 3：Agent 装配与提示词构建解耦

目标：

- 减少 `ChatService` 和 `AiOpsService` 中重复的 Agent 装配逻辑

计划动作：

- 提取 `AgentToolRegistry`
  - 管理 method tools 和 MCP tools
- 提取 `AgentFactory`
  - 创建 Chat Agent
  - 创建 Planner Agent
  - 创建 Executor Agent
  - 创建 Supervisor Agent
- 提取 `PromptBuilder`
  - `ChatPromptBuilder`
  - `AiOpsPlannerPromptBuilder`
  - `AiOpsExecutorPromptBuilder`
  - `AiOpsSupervisorPromptBuilder`

阶段结果：

- Agent 初始化逻辑统一
- Prompt 可集中管理、版本化和调试


### 阶段 4：基础设施适配层拆分

目标：

- 将真实 API 接入、Mock 数据、SDK 调用细节从业务类中拆出去

计划动作：

- Prometheus
  - `PrometheusAlertClient`
  - `MockPrometheusAlertClient`
- 日志查询
  - `LogQueryClient`
  - `MockLogQueryClient`
  - `TencentClsLogQueryClient` 或 `McpLogQueryClient`
- 向量能力
  - `EmbeddingClient`
  - `DashScopeEmbeddingClient`
  - `VectorRepository`
  - `MilvusVectorRepository`

阶段结果：

- Mock 和真实实现可以自由切换
- 业务层不感知具体 SDK
- 后续换平台成本更低


### 阶段 5：知识库链路整理

目标：

- 让文档上传、切片、索引、检索形成稳定且可扩展的链路

计划动作：

- 抽象文档入库流程
  - 文件校验
  - 文件存储
  - 文档切片
  - 向量生成
  - 向量写入
- 提取 `DocumentIngestionService`
- 提取 `DocumentStorageService`
- 统一索引结果对象
- 明确 `RagService` 的定位
  - 若继续保留：正式接入 API
  - 若不作为主路径：降级为内部服务

阶段结果：

- 上传知识库文档的行为可控
- RAG 与 Agent 检索边界更明确


### 阶段 6：前端与配置工程化

目标：

- 提升本地开发体验和配置可维护性

计划动作：

- 前端改为相对路径访问 API
- 补充前端错误提示和连接失败说明
- 梳理 `application.yml`
- 增加配置分层
  - `application-dev.yml`
  - `application-local.yml`
- 增加配置校验和启动提示

阶段结果：

- 本地开发更稳定
- 后续迁移环境时更容易


### 阶段 7：测试与质量保障

目标：

- 在完成结构清理后逐步补测试

计划动作：

- 单元测试
  - 文档分片
  - 会话窗口裁剪
  - Prompt 构建
- 集成测试
  - 控制器返回格式
  - 文件上传流程
  - SSE 输出结构

阶段结果：

- 后续继续改代码时更安全


## 8. 第一批建议优先实现项

建议先从以下内容开始，风险最低，收益最大：

1. 统一 `ApiResponse`、`SseMessage`、请求 DTO、响应 DTO
2. 拆分 `ChatController`
3. 提取 `SessionService`
4. 前端改成相对路径 API
5. 将 `@Autowired` 逐步改为构造器注入

这几项完成后，整体代码可读性会明显提升，也更适合后续继续接真实 API。


## 9. 后续需要你提供的配置 / API 信息

为了把当前示例能力逐步落地成真实可用能力，后续实现中需要你提供以下信息。

### 9.0 当前已确认配置

以下信息已经确认，可作为后续实施的默认基线：

- DashScope API Key 通过 macOS shell 环境变量提供
- 环境变量名为 `DASHSCOPE_API_KEY`
- 腾讯云 MCP 使用 SSE 连接
- public 仓库中不保留具体 SSE endpoint，改为本地环境变量配置

说明：

- 后续代码改造时，应优先继续使用环境变量 `DASHSCOPE_API_KEY`
- 后续接入 MCP 时，SSE endpoint 应通过 `TENCENT_MCP_SSE_ENDPOINT` 注入
- MCP base URL 可通过 `TENCENT_MCP_BASE_URL` 配置，默认值为 `https://mcp-api.tencent-cloud.com`
- 若后续还需要额外鉴权头或额外参数，再补充到本地配置中

### 9.1 DashScope / LLM 相关

- 聊天模型名称
- Embedding 模型名称
- 是否需要固定超时、重试次数、温度参数


### 9.2 Milvus 相关

- Milvus host / port
- 是否需要用户名密码
- 是否使用默认 database
- 集合名称是否保持 `biz`
- 是否需要重新建 collection / index


### 9.3 Prometheus 相关

- Prometheus base URL
- 是否有鉴权
- 真实告警接口格式是否标准 `/api/v1/alerts`
- 超时要求


### 9.4 日志平台相关

请根据后续你准备使用的方式提供：

- 腾讯云 CLS 真实 API 信息，或
- MCP 服务地址与鉴权方式，或
- topic / region / query 规范

至少需要：

- 默认 region
- 日志主题或 topicId
- 鉴权方式（如无额外鉴权，也请明确）
- 真实查询参数格式


### 9.5 前端和部署相关

- 最终前后端是否同域部署
- 服务端口是否继续使用 `9900`
- 是否需要限制 CORS 域名
- 是否保留当前静态页面


## 10. macOS 本地开发环境建议

当前用户环境为 macOS，建议统一使用以下本地开发依赖：

### 10.1 必备软件

- JDK 17
- Maven 3.9+
- Docker Desktop for Mac
- Git

### 10.2 推荐安装方式

如果你使用 Homebrew，建议：

```bash
brew install openjdk@17
brew install maven
brew install git
```

Docker 请安装：

- Docker Desktop for Mac

### 10.3 建议检查命令

```bash
java -version
mvn -version
docker --version
docker compose version
```

### 10.4 本地运行建议

建议采用以下启动顺序：

1. 启动 Milvus 相关容器
2. 配置环境变量
3. 启动 Spring Boot
4. 上传文档并验证知识库

示例：

```bash
export DASHSCOPE_API_KEY=your-key
docker compose -f vector-database.yml up -d
mvn spring-boot:run
```


## 11. 风险与注意事项

### 11.1 不建议一次性重写

不建议直接重写为全新项目结构，原因：

- 当前已有能工作的主链路
- 一次性重写风险高
- 前端和接口容易同时失配


### 11.2 先保证接口兼容

重构前几阶段，尽量保持以下接口路径不变：

- `/api/chat`
- `/api/chat_stream`
- `/api/ai_ops`
- `/api/chat/clear`
- `/api/chat/session/{sessionId}`
- `/api/upload`


### 11.3 真实 API 接入要晚于结构重构

建议先把结构整理好，再接真实外部 API。否则会出现：

- 一边改结构一边查接入问题
- 难以区分是架构问题还是配置问题


## 12. 验收标准

当完成第一轮重构后，至少应达到以下标准：

- 控制器职责清晰，不再出现单文件承载所有流程
- DTO、响应结构、异常处理统一
- 会话管理从控制器中抽离
- Agent 创建和 Prompt 构建不再分散重复
- 前端不再写死 API 地址
- 本地 macOS 环境可按文档完成启动


## 13. 后续实施方式

后续每次实现建议按以下方式推进：

1. 先确认这一轮要改的阶段或子任务
2. 由你提供本轮需要的配置 / API 信息
3. 只改一小块，并完成自检
4. 输出改动说明和下一步建议

建议的实施顺序：

1. 阶段 1：接口层瘦身与统一模型
2. 阶段 2：会话与流式输出解耦
3. 阶段 3：Agent 装配与提示词构建解耦
4. 阶段 4：基础设施适配层拆分
5. 阶段 5：知识库链路整理
6. 阶段 6：前端与配置工程化
7. 阶段 7：测试与质量保障


## 14. 当前结论

这个项目适合“渐进式重构”，不适合“推倒重来”。

接下来建议先从第一阶段开始，优先整理接口层、DTO、响应格式和会话管理。等你提供对应配置或平台信息后，再逐步接入真实 API 和完善基础设施适配层。
