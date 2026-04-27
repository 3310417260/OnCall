# OpsPilot AI 重构计划

## 1. 文档目的

本文档用于同步 `OpsPilot AI` 当前重构进展、剩余问题和后续改造优先级。

这份文档不再只是最初的“规划草案”，而是按当前仓库真实状态持续更新的工作清单，目标是：

- 记录已经完成的结构整理
- 对齐当前代码、README、架构文档和面试口径
- 明确下一步最值得投入的改造点
- 为后续继续推演到更稳定的 OnCall / AIOps 系统留出清晰路径


## 2. 当前项目定位

项目当前是一个面向 OnCall / AIOps 场景的智能助手系统，核心整合了三类能力：

- 基于 RAG 的内部知识检索
- 基于 ReAct 的普通对话问答
- 基于 Supervisor + Planner + Executor 的 AIOps 工作流

当前重点已经从“能不能跑通”逐步转向“边界是否清晰、结构是否可持续演进”。


## 3. 当前重构进展总览

### 3.1 已完成的结构性改造

以下内容已经完成并落到代码中：

1. 控制器拆分
   - `ChatController`
   - `AiOpsController`
   - `SessionController`
   - `FileUploadController`

2. DTO 与统一响应抽离
   - `ApiResponse`
   - `ChatRequest`
   - `ChatResponse`
   - `ClearSessionRequest`
   - `SessionInfoResponse`
   - `SseMessage`
   - `IndexTaskResponse`
   - `FileUploadRes`

3. 会话管理抽离
   - `SessionService`

4. Prompt 分层
   - `ChatPromptService`
   - `AiOpsPromptService`
   - `ChatPromptBuilder`
   - `AiOpsPlannerPromptBuilder`
   - `AiOpsExecutorPromptBuilder`
   - `AiOpsSupervisorPromptBuilder`
   - `AiOpsTaskPromptBuilder`

5. Agent 装配收口
   - `AgentToolRegistry`
   - `AgentFactory`

6. 上传索引改造
   - 文档上传改为异步索引任务
   - 增加索引任务状态查询接口

7. 稳定性增强
   - `VectorEmbeddingService` 增加重试与退避
   - 增加 `GlobalExceptionHandler`
   - 增强前端错误提示

8. 配置工程化改进
   - 前端 API 已改为相对路径
   - MCP SSE endpoint 改为环境变量注入
   - DashScope API Key 使用环境变量

9. 文档补充
   - `ARCHITECTURE.md`
   - `INTERVIEW_QA.md`
   - `TOOL_INVENTORY.md`


### 3.2 已完成但仍可继续增强的部分

以下工作已经有初步落地，但还没到“最终形态”：

1. AIOps 多 Agent 边界
   - 已经引入 `Supervisor + Planner + Executor`
   - 已经按角色拆分工具权限
   - 已开始引入 `PlannerPlan` / `ExecutorFeedback` DTO 与解析校验服务
   - 但 workflow guardrails 和完整状态机约束仍未完全下沉到代码层

2. 全局异常治理
   - `GlobalExceptionHandler` 已经接入
   - 但 SSE 场景和更细粒度的业务异常仍可以继续统一

3. 异步索引链路
   - 后端已支持任务状态查询
   - 前端已补轮询提示
   - 但知识入库模块边界仍可继续抽象

4. Prompt Builder 分层
   - 已经把 prompt 从 service 中剥离
   - 但 prompt 版本化、schema 化约束仍未建立


## 4. 当前仍然存在的主要问题

### 4.1 多 Agent 中间状态仍不够结构化

虽然已经把 Planner 和 Executor 的职责开始分清，但当前状态传递仍主要依赖文本型 prompt 和自由文本输出：

- `planner_plan`
- `executor_feedback`

问题：

- replan / finish 判断过于依赖 LLM 自身理解
- 工作流状态难以做硬性校验
- 不利于后续加 verifier、最大轮数、失败恢复


### 4.2 基础设施适配层还没有真正拆开

当前仍存在一些“工具类直接碰 SDK / HTTP / mock 逻辑”的情况：

- `QueryMetricsTools`
- `QueryLogsTools`
- `VectorEmbeddingService`
- `VectorIndexService`

问题：

- mock 与真实实现还没有彻底分层
- 底层平台替换成本偏高
- 单元测试仍然不够容易


### 4.3 知识库链路还缺明确分层

当前文档入库已经变成异步任务，但仍可继续明确：

- 文件存储
- 切片
- 向量生成
- 向量写入
- 检索输出结构

问题：

- `RagService` 的定位仍偏模糊
- 上传、索引、检索模块之间的边界还能再清晰


### 4.4 配置分层还没有真正落地

虽然已经处理了部分敏感配置，但还缺：

- `application-local.yml`
- `application-dev.yml`
- 更清晰的配置校验
- 启动前依赖检查


### 4.5 自动化测试仍不足

当前仍然缺少系统性的：

- 单元测试
- 集成测试
- 工作流回归测试
- prompt / tool 行为回归测试


## 5. 当前结构理解

从当前代码来看，项目已经逐步演进为如下形态：

```text
Controller
-> Service / Workflow
-> Prompt Builder / Agent Factory / Tool Registry
-> Tool / Vector / Integration
-> External Systems
```

更具体一点：

- HTTP 层负责接口分流与返回结构
- `ChatService` / `AiOpsService` 负责核心流程
- `AgentFactory` / `AgentToolRegistry` 负责 Agent 装配
- Prompt Builder 负责行为约束
- Tool 层负责知识库、Prometheus、日志等能力暴露
- Milvus / DashScope / MCP / Prometheus / CLS 属于外部能力


## 6. 已确认的编排策略

这部分是当前项目设计里已经基本定型的共识，后续改造应尽量保持一致。

### 6.1 普通问答

- 使用单个 `ReactAgent`
- 以 tool invocation 方式调用知识库、Prometheus、日志等能力
- 不需要引入多 Agent workflow

### 6.2 知识库

- 知识入库不是 Agent，而是异步索引 pipeline
- 知识检索不是独立 Agent，而是 `queryInternalDocs` tool

### 6.3 AIOps

- 外层使用 `SupervisorAgent` 做 workflow orchestration
- 内层使用 `planner_agent` 和 `executor_agent`
- `Planner` 偏策略、知识与告警理解
- `Executor` 偏日志与指标取证

### 6.4 MCP

- 当前项目使用的是 Spring AI `MCP Client`
- 项目本身不实现 MCP Server
- 远端能力通过 `ToolCallbackProvider` 注入本地 Agent


## 7. 重构路线图（重新编排版）

下面的路线图按 `短期 / 中期 / 长期` 重新整理，优先级依据不是“概念上是否高级”，而是：

- 能否直接提升当前系统可控性
- 能否减少后续继续重构时的返工
- 能否明显提升项目表达和面试可解释性


### 7.1 短期：把 workflow 从“能跑”变成“更可控”

这一阶段是当前最值得投入的部分，因为它直接命中项目现在最明显的短板：Planner / Executor 仍然偏软约束。

#### 目标 1：中间状态 schema 化

把：

- `planner_plan`
- `executor_feedback`

从“JSON 风格文本”推进到“代码可识别、可校验的结构化状态”。

建议动作：

- 定义 `PlannerPlan` DTO
  - `decision`
  - `step`
  - `tool`
  - `context`
  - `reason`
- 定义 `ExecutorFeedback` DTO
  - `status`
  - `summary`
  - `evidence`
  - `error`
  - `nextHint`
- 在 prompt 中继续保留格式说明
- 在代码层补 JSON 解析与基础字段校验

预期收益：

- 让“结构化输出”从纯 prompt 软约束推进到半硬约束
- 为 replan / finish 规则下沉打基础
- 明显提升多 Agent 工作流的可解释性

#### 目标 2：workflow guardrails 下沉

避免所有流程控制都完全依赖模型自由判断。

建议动作：

- 增加最大轮数
- 增加连续失败阈值
- 增加“无新增证据”阈值
- 增加 finish 的最低条件
- 在必要时输出“无法完成”的明确状态，而不是让模型硬收尾

预期收益：

- 减少语义漂移导致的失控
- 让 AIOps workflow 更接近工程系统，而不是纯 prompt demo
- 更容易回答“为什么这样设计 workflow”

#### 目标 3：补轻量 verifier / reviewer

建议动作：

- 先以轻量校验器形式实现
- 检查最终结论是否引用过证据
- 检查工具失败后是否仍强行给出确定性结论

预期收益：

- 让 workflow 多一层质量保护
- 提升项目对“大模型不稳定性”的治理能力


### 7.2 中期：把基础设施和知识库边界拆干净

这一阶段的重点不是“再加功能”，而是让当前代码更像一个能持续演进的工程系统。

#### 目标 4：基础设施适配层拆分

当前项目已经暴露出这类问题：

- 工具类直接碰 SDK / HTTP
- mock 和真实实现还没有完全分层
- service 还承担了过多底层细节

建议动作：

- Prometheus
  - `PrometheusAlertClient`
  - `MockPrometheusAlertClient`
- 日志
  - `LogQueryClient`
  - `MockLogQueryClient`
  - `McpLogQueryClient`
- Embedding
  - `EmbeddingClient`
  - `DashScopeEmbeddingClient`
- Vector
  - `VectorRepository`
  - `MilvusVectorRepository`

预期收益：

- mock / 真实实现边界清晰
- 更容易写测试
- 更容易替换平台

#### 目标 5：知识库链路进一步整理

建议动作：

- 提取 `DocumentIngestionService`
- 视需要补 `DocumentStorageService`
- 明确 `RagService` 是保留为备用链路，还是正式退出主路径
- 统一检索结果对象
- 增加引用信息，便于回答时带出处
- 第一阶段规则型 rerank 已落地：
  - 新增 `KnowledgeRetrievalService`
  - 使用 `RecallCandidate / RetrievedChunk` 统一召回与最终结果对象
  - 在服务层基于 `content + metadata(_source/_file_name/title/chunkIndex/totalChunks)` 做重排
  - 增加同源 chunk 惩罚与单来源保留上限
  - 当前默认仍关闭 `rag.rerank.enabled=false`，用于兼容旧链路并便于灰度联调
- 下一阶段继续补：
  - 检索 trace 与失败 case 诊断
  - 权重调优与阈值调优
  - 模型型 rerank provider
  - 更稳定的中文关键词策略
  - 检索质量评估与 trace

预期收益：

- 让上传、切片、向量化、写库、检索形成稳定边界
- 让 RAG 不只是“能搜到”，而是“更适合回答”

#### 目标 6：配置与环境分层

建议动作：

- 增加 `application-local.yml` 示例
- 增加 `application-dev.yml`
- 增加关键配置校验
- 增加启动前依赖检查
- 明确本地 mock / 真实环境切换方式

预期收益：

- 提升本地开发体验
- 降低环境切换成本


### 7.3 长期：把系统从“可运行”推进到“可治理”

这一阶段不是马上要做，但它代表这个项目未来的演进方向。

#### 目标 7：吸收前沿工程范式（Harness / LLM Wiki）

这部分属于“前沿技术尝试”，适合在当前主链路稳定之后逐步吸收，而不是替代现有架构。

##### 方向 A：Harness Engineering 思路落地

这里的重点不是引入某个特定框架，而是吸收其核心思想：给 Agent 提供更强的约束、反馈和运行护栏。

适合在本项目中落地的点：

- 把 workflow 的输入输出 contract 文档化
- 把 Planner / Executor 中间状态变成可校验协议
- 增加 tool trace、step trace 和简单诊断面板
- 增加固定问题集和回归评测
- 把“规则、约束、测试、观测”变成 Agent 可依赖的运行环境

判断标准：

- 如果某项工程在帮模型减负、减少自由发挥，就值得优先尝试
- 如果某项工程明显增加了 prompt 复杂度和上下文复杂度，则应谨慎推进

##### 方向 B：Karpathy 式 LLM Wiki / Knowledge Layer

这里的重点不是替换现有 Milvus RAG，而是增加一层“可沉淀、可维护、可复用”的知识层。

适合本项目的尝试方式：

- 保留现有“原始文档 + 向量检索”链路
- 增加一层高价值知识沉淀：
  - 常见告警处理流程
  - 高频 incident 经验
  - 精炼后的 runbook
  - 事故复盘摘要
- 让 Agent 优先查询 wiki 化知识，再决定是否回源查原始文档

推荐策略：

- 先做小范围试点，不要整体替换现有知识库链路
- 优先沉淀高频 OnCall 知识，而不是所有原始文档

预期收益：

- 让知识不再完全依赖 query-time RAG
- 提升高频问题的稳定回答质量
- 增强项目对“长期知识积累”的支持能力

#### 目标 8：MCP 能力边界继续完善

当前项目主要消费的是 MCP `tools`，这是合理的，因为项目现阶段最需要的是远端可执行能力。

后续可以继续思考：

- 哪些固定上下文适合抽成 MCP `resources`
- 哪些任务模板适合本地 Prompt Builder 持续管理
- 是否需要把部分稳定流程沉淀为 MCP `prompts`

这里的原则是：

- 不要为了“用全 MCP”而生搬硬套
- 只有当远端资源或模板确实具备复用价值时，才考虑扩展

#### 目标 9：评估与回归体系

建议动作：

- 单元测试
  - `SessionService`
  - `DocumentChunkService`
  - Prompt Builder
  - `DocumentIndexTaskService`
- 集成测试
  - 控制器返回格式
  - 上传任务状态
  - SSE 输出结构
- workflow 回归
  - tool 调用链路
  - AIOps workflow 基本成功率
  - 检索质量回归

#### 目标 10：交叉验证与置信度治理

当前项目已经开始具备 verifier 雏形，但还没有形成独立的 confidence / cross-check 层。

建议动作：

- 增加 `AnswerConfidenceService` 或 `RetrievalVerificationService`
- 对以下信号做统一打分：
  - 检索质量
    - top1/top3 分数稳定性
    - 标题/文件名命中情况
    - 多个 chunk 是否支撑同一主题
  - 证据一致性
    - 知识库、Prometheus、日志是否相互印证
    - `ExecutorFeedback.status` 与证据内容是否一致
  - 输出质量
    - 最终回答/报告是否带出处
    - 是否存在明显超出证据的强结论
- 输出 `HIGH / MEDIUM / LOW` 级别置信度
- 在低置信度场景下触发降级策略：
  - 提示“当前证据不足”
  - 建议人工复核
  - 触发二次检索或 query rewrite

预期收益：

- 让系统不仅“给答案”，还知道“答案有多可信”
- 减少检索偏差被直接放大成错误结论
- 为后续 answer verification 和多证据校验打基础

#### 目标 11：多轮对话上下文压缩与记忆分层

当前聊天上下文仍是简单滑动窗口：`SessionService` 保留最近 6 轮问答，并由 `ChatPromptBuilder` 直接把历史拼进 system prompt，还没有单独的摘要压缩层。

建议动作：

- 短期：
  - 保持滑动窗口，但增加 token/字符预算感知
  - 当历史超限时，不只是截断，而是先提取最近关键问答摘要
- 参考分级上下文治理策略，逐步引入以下层级：
  - 第一层：旧对话只保留任务框架
    - 用户目标
    - 已确认事实
    - 已执行动作
    - 当前待解决问题
  - 第二层：工具执行结果卸载
    - 不保留完整日志/完整检索结果
    - 只保留工具名、关键结论、证据摘要和引用
  - 第三层：上下文折叠
    - 最近几轮保留 raw messages
    - 更早对话转为 folded / summarized memory
  - 第四层：接近上下文窗口上限时触发全文压缩
    - 根据 token/字符预算压缩整段历史
    - 不再只按固定轮数裁剪
  - 第五层：紧急保护
    - 当上下文过长导致无法安全继续时，强制执行 emergency compression
    - 必要时向上层返回“上下文已自动压缩/需重新聚焦问题”的信号
- 中期：
  - 增加 `ConversationSummaryService`
  - 让旧历史压缩成 summary memory，近期历史保留 raw memory
  - 形成：
    - short-term memory：最近几轮原始对话
    - summary memory：较早对话摘要
    - tool evidence memory：工具结果摘要
- 长期：
  - 按主题/任务拆分会话记忆
  - 对重复上下文做去重
  - 结合检索型 memory，而不是把全部历史都塞进 prompt

预期收益：

- 降低多轮对话上下文膨胀
- 保留关键上下文而不是简单丢弃旧消息
- 提升长会话场景下的稳定性和回答一致性

预期收益：

- 让后续继续改 prompt、改 workflow、改工具边界时更安全

#### 目标 10：更完整的观测能力

建议动作：

- 记录 tool 调用 trace
- 记录 Planner / Executor 中间状态
- 记录 workflow 每轮决策
- 统计检索命中率、工具成功率、响应耗时

预期收益：

- 让系统从“能用”进一步变成“可观测、可诊断”


## 8. 当前建议的执行顺序

如果只看现在最值得继续做什么，我建议按下面顺序推进：

1. `planner_plan` schema 化
2. `executor_feedback` schema 化
3. replan / finish guardrails 下沉
4. 轻量 verifier / reviewer
5. Prometheus / 日志 / Embedding / Vector 适配层拆分
6. 知识库链路进一步整理
7. 配置分层与测试补齐
8. 视稳定性情况引入 Harness 化治理能力
9. 小范围试点 LLM Wiki / 高价值知识沉淀层

这条顺序的核心思路是：

- 先补 workflow 治理层
- 再补基础设施边界
- 最后再做前沿范式试点和长期治理能力


## 9. 环境与配置现状

### 9.1 当前已确认配置

- `DASHSCOPE_API_KEY` 已通过 macOS 环境变量提供
- `TENCENT_MCP_SSE_ENDPOINT` 用于注入私有 SSE endpoint
- `TENCENT_MCP_BASE_URL` 默认值为 `https://mcp-api.tencent-cloud.com`
- Milvus 当前通过 `vector-database.yml` 与 Docker Compose 启动
- Spring Boot 默认端口仍为 `9900`

### 9.2 当前本地运行建议

```bash
export DASHSCOPE_API_KEY=your-key
export TENCENT_MCP_SSE_ENDPOINT=/sse/your-private-endpoint
docker compose -f vector-database.yml up -d
mvn spring-boot:run
```

### 9.3 当前仍需补充的真实环境信息

- DashScope 聊天模型名
- DashScope embedding 模型名
- Prometheus 真实地址与鉴权方式
- 日志查询的真实 topic / region / 约束
- 是否需要额外的 MCP 鉴权头


## 10. 验收标准

下一轮重构完成后，建议至少满足以下标准：

1. Planner / Executor 中间状态结构化
2. replan / finish 不再完全依赖 prompt 自由判断
3. 工具层开始出现清晰的 client / repository 边界
4. 文档、代码、面试口径三者一致
5. 关键链路至少有基础测试或回归验证


## 11. 当前结论

项目已经完成了第一轮高价值结构整理，当前不再适合“推倒重来”，而适合继续做两类深入改造：

- 一类是把 AIOps workflow 从“可运行”推进到“更可控”
- 一类是把基础设施接入从“能用”推进到“更清晰”

后续最推荐的下一步不是再加新功能，而是：

1. 先做 `planner_plan / executor_feedback` schema 化
2. 再做 replan / finish 规则下沉
3. 然后拆 Prometheus / 日志 / Embedding / Vector 适配层

这三步做完后，项目在结构、可维护性和面试表达上都会更完整。
