# 项目问答整理

这份文档整理了在“这个项目 Agent 之间的通信到底是怎么实现的？”之后延伸出来的一组关键问题，目的是帮助快速复盘项目设计、统一口径，并在面试或项目讲解时更稳地回答追问。

## 1. Agent 之间的通信到底是怎么实现的？

要先区分三种关系，不能都笼统地叫“Agent 通信”。

### 1.1 对话 Agent 和知识库之间

这不是 agent-to-agent 通信，而是 **agent-to-tool 调用**。

- 普通问答走单个 `ReactAgent`
- 需要查知识时，Agent 直接调用 `queryInternalDocs`
- `queryInternalDocs` 再进入向量检索链路，返回检索结果给 Agent

对应代码：

- [ChatController.java](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/java/org/example/controller/ChatController.java:1)
- [InternalDocsTools.java](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/java/org/example/agent/tool/InternalDocsTools.java:1)
- [VectorSearchService.java](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/java/org/example/service/VectorSearchService.java:1)

更准确的说法是：

> 对话 Agent 和知识库之间不是共享上下文通信，而是 ReAct Agent 通过 tool invocation 同步调用知识库检索，检索结果作为 observation 回到 Agent。

### 1.2 Planner 和 Executor 之间

这里才是真正意义上的多 Agent 间接通信。

- `planner_agent` 输出写入 `planner_plan`
- `executor_agent` 输出写入 `executor_feedback`
- `SupervisorAgent` 负责决定下一步调用谁
- 子 Agent 通过共享工作流状态间接通信

对应代码：

- [AgentFactory.java](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/java/org/example/service/AgentFactory.java:1)
- [AiOpsService.java](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/java/org/example/service/AiOpsService.java:1)

更准确的说法是：

> Agent 之间通过 Supervisor 管理的共享工作流状态进行间接通信，核心状态槽位是 `planner_plan` 和 `executor_feedback`。

### 1.3 对话 Agent 和执行 Agent 之间

当前实现里，这两者 **没有直接交互**。

- 对话 Agent 属于 `/api/chat` 工作流
- Executor 属于 `/api/ai_ops` 工作流

所以更准确的回答是：

> 当前项目里普通对话 Agent 和 AIOps 的执行 Agent 分属不同 workflow，并不直接通信。

## 2. 知识库到底是不是一个 Agent？

不是。更合适的定义是：

- 知识入库：异步索引流水线
- 知识检索：一个 tool

当前项目里最不适合说成 Agent 的，就是知识入库这条链路：

- 上传文件
- 文档切分
- embedding
- Milvus 入库

这本质上是确定性的服务流水线，而不是需要自主决策的 Agent。

对应代码：

- [FileUploadController.java](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/java/org/example/controller/FileUploadController.java:1)
- [DocumentIndexTaskService.java](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/java/org/example/service/DocumentIndexTaskService.java:1)
- [VectorIndexService.java](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/java/org/example/service/VectorIndexService.java:1)

更稳的表述是：

> 知识库入库是异步索引 pipeline，知识检索作为 tool 接给上层 Agent 使用，而不是把知识层本身 Agent 化。

## 3. 为什么对话用 ReAct，而 AIOps 要拆成 Plan-Execute-Replan？

因为任务复杂度不同。

### 3.1 ReAct 适合什么

普通对话通常是：

- 理解用户问题
- 判断要不要调工具
- 调一次或几次工具
- 直接给出答案

这种任务用单个 `ReactAgent` 就足够了。

### 3.2 Plan-Execute-Replan 适合什么

AIOps 排障往往是明显的多步任务：

- 先规划排查路径
- 执行一步
- 根据结果修正计划
- 最后汇总结论

如果全部塞给一个 Agent，容易出现计划不清、工具乱调、没有纠偏机制的问题。

更准确的表述是：

> 对话场景用单个 ReAct Agent，AIOps 场景使用外层 Plan-Execute-Replan workflow。当前项目里，外层是 Supervisor 驱动的工作流，内层 Planner 和 Executor 底层仍然是 ReactAgent。

## 4. 外层 workflow orchestration 是什么？项目里有实现吗？

有实现，指的是：

- 不是一个 Agent 从头到尾做完
- 而是有一个总控决定先谁执行、后谁执行、什么时候结束

在这个项目里，外层编排由 `SupervisorAgent` 实现。

它主要负责：

- 调用 `planner_agent`
- 调用 `executor_agent`
- 基于中间状态决定是否继续规划或结束

对应代码：

- [AgentFactory.java](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/java/org/example/service/AgentFactory.java:1)
- [AiOpsPromptService.java](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/java/org/example/prompt/AiOpsPromptService.java:1)

更准确的说法是：

> 外层 orchestration 做流程调度，内层 Planner 和 Executor 再按 ReAct 风格决定是否调用具体工具。

## 5. 执行到什么程度要不要 replan？当前是谁决定的？

当前实现里，这部分 **主要是 LLM 驱动判断**。

- Planner prompt 会要求输出 `PLAN | EXECUTE | FINISH`
- Supervisor prompt 再根据这些信号决定下一步调谁
- `executor_feedback` 作为下一轮 Planner 的输入

也就是说，现在还没有特别硬的代码规则，例如：

- 执行失败几次必须 replan
- 最大轮数
- 最低证据数量

所以当前设计是：

> prompt 约束为主，代码层只做轻量编排。

### 当前设计合理吗？

作为 PoC 或演示项目是合理的，因为它足够体现多 Agent workflow。

### 还可以怎么改进？

推荐的改进方向：

- 把 `planner_plan` / `executor_feedback` schema 化
- 增加最大轮数和失败阈值
- 增加显式的 finish / replan 判断条件
- 给 Planner 和 Executor 更清晰的工具权限边界
- 加一个 verifier / reviewer 角色做最终校验

## 6. Supervisor、Planner、Executor 到底谁调用知识库？

当前实现里：

- `SupervisorAgent` 不直接挂知识库工具
- `planner_agent` 挂了知识库工具
- `executor_agent` 不再直接挂知识库工具，而是更偏日志和指标取证工具

也就是说，并不是 Supervisor 先查知识库再把干净信息喂给子 Agent，而是：

- Supervisor 负责编排
- Planner 负责用知识库、告警概览和告警元信息判断方向
- Executor 负责执行当前步骤，优先做日志和指标取证

对应代码：

- [AgentFactory.java](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/java/org/example/service/AgentFactory.java:1)
- [AgentToolRegistry.java](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/java/org/example/service/AgentToolRegistry.java:1)

更准确的说法是：

> 当前项目里知识库主要由 Planner 调用，用于制定和修正排查方向；Executor 主要使用日志和指标工具完成当前步骤的取证，而不是与 Planner 完全共享同一套工具。

## 7. MCP 在这个项目里怎么用？项目里有没有 MCP Server？

当前项目里使用的是 **Spring AI 的 MCP Client**，不是 MCP Server。

### 7.1 这个项目做了什么

- 通过配置文件连接远端 MCP Server
- 发现远端暴露出来的工具
- 把这些工具包装成 `ToolCallback`
- 再挂载到本地 Agent 的工具集合里

对应代码：

- [application.yml](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/resources/application.yml:1)
- [AgentToolRegistry.java](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/java/org/example/service/AgentToolRegistry.java:1)
- [AgentFactory.java](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/java/org/example/service/AgentFactory.java:1)

### 7.2 这个项目没做什么

- 没有自己实现 MCP Server
- 没有自己维护 MCP 工具注册表
- 没有自己实现 MCP 协议服务端

更准确的表述是：

> 当前项目是 MCP Client 侧集成方，通过 Spring AI MCP Client 把远端工具接进本地 Agent。

## 8. MCP、tool description、skill 之间是什么关系？

这三者不是一回事。

- MCP：工具接入协议
- tool description：单个工具的能力说明
- skill：任务层的使用策略

### 8.1 MCP 解决什么

解决的是：

- 工具怎么发现
- 工具怎么接入
- 调用协议怎么统一

### 8.2 tool description 解决什么

解决的是：

- 模型怎么理解单个工具的用途和参数

### 8.3 skill 更应该是什么

真正的 skill 不只是“把工具说明写进自然语言”，而应该包含：

- 任务目标
- 调用顺序
- 失败处理
- 输出格式
- 终止条件

更准确的总结是：

> MCP 提供可调用能力，tool description 帮模型理解单个工具，skill 负责在具体任务中组织这些能力如何被使用。

## 9. 这个项目里到底有没有独立 skill 层？

严格说，没有一个显式的独立 skill 模块。

当前项目里最接近 skill 的部分其实是：

- Prompt Builder
- AIOps workflow 编排规则

对应代码：

- [ChatPromptBuilder.java](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/java/org/example/prompt/builder/ChatPromptBuilder.java:1)
- [AiOpsPlannerPromptBuilder.java](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/java/org/example/prompt/builder/AiOpsPlannerPromptBuilder.java:1)
- [AiOpsExecutorPromptBuilder.java](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/java/org/example/prompt/builder/AiOpsExecutorPromptBuilder.java:1)
- [AiOpsSupervisorPromptBuilder.java](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/java/org/example/prompt/builder/AiOpsSupervisorPromptBuilder.java:1)

更准确的说法是：

> 这个项目当前是 “本地 Tool + MCP Tool + Prompt/Workflow 编排” 的实现，而不是一套独立 Skill 系统。

## 10. 项目里 MCP 调用远程服务的文件在哪里？

MCP 远程调用不是通过手写 HTTP 文件完成的，而是 Spring AI MCP Client 自动完成的。

关键落点：

- MCP 配置入口：[application.yml](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/resources/application.yml:1)
- 读取远端工具回调：[AgentToolRegistry.java](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/java/org/example/service/AgentToolRegistry.java:1)
- 挂到 Agent：[AgentFactory.java](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/java/org/example/service/AgentFactory.java:1)

当前项目源码里没有你自己手写的 “MCP SSE HTTP 调用文件”，底层 transport 在 Spring AI 框架内部。

## 11. `toolCallbackProvider.getToolCallbacks()` 到底是什么？

它不是读取本地某个 Markdown 文件，而是在运行时拿 Spring AI 已经准备好的 `ToolCallback[]`。

这些 callback 的来源是：

- 应用启动
- Spring AI MCP Client 连接远端 MCP Server
- 发现远端 tools
- 包装成 `ToolCallback`
- 注入到 `ToolCallbackProvider`

更准确地说：

> `toolCallbackProvider.getToolCallbacks()` 拿到的是运行时装配好的远端工具回调集合，而不是某个本地文档。

## 12. 用 SSE 调用是什么意思？SSE 不就是流式输出吗？

SSE 确实是一种流式传输方式，但在这个项目里有两种用途。

### 12.1 前端接口里的 SSE

用于把模型输出分片返回给前端，例如：

- `/api/chat_stream`
- `/api/ai_ops`

### 12.2 MCP 里的 SSE

用于 Spring AI MCP Client 和远端 MCP Server 之间维持连接，作为协议传输通道。

更准确的说法是：

> SSE 是一种传输方式。前端场景里它承载模型流式输出，MCP 场景里它承载 Client 和 Server 之间的协议通信。

## 13. 项目里的对话算不算交互式输出？

算，但要区分“交互式输出”和“流式输出”。

- 交互式输出：强调多轮上下文和持续问答
- 流式输出：强调结果分片返回

这个项目：

- `/api/chat`：交互式，但不是流式
- `/api/chat_stream`：交互式 + 流式
- `/api/ai_ops`：交互式 + 流式

对应代码：

- [SessionService.java](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/java/org/example/service/SessionService.java:1)
- [ChatController.java](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/java/org/example/controller/ChatController.java:1)
- [AiOpsController.java](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/java/org/example/controller/AiOpsController.java:1)

## 14. 相似检索使用的是什么策略？

当前项目里的知识检索是典型的 **chunk-level dense retrieval**。

整体流程：

- 把 query 做 embedding
- 在 Milvus 做 TopK 向量检索
- 距离度量使用 `L2`
- 搜索参数里有 `nprobe=10`

对应代码：

- [VectorSearchService.java](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/java/org/example/service/VectorSearchService.java:1)

更准确的表述是：

> 当前项目使用向量相似度检索策略，在 chunk 级别做 dense retrieval，底层用 Milvus 做 TopK ANN 搜索，距离度量采用 L2。

## 15. Prometheus 在这个项目里到底干了什么？

Prometheus 在这个项目里不是监控系统本身，而是 **告警信息来源**。

当前工具：

- `queryPrometheusAlerts`

它主要做的是：

- mock 模式下返回内置告警
- 非 mock 模式下调用 `/api/v1/alerts`

对应代码：

- [QueryMetricsTools.java](/Users/yuxinfeng/code/Agent/SuperBizAgent-release/src/main/java/org/example/agent/tool/QueryMetricsTools.java:1)

项目里主要围绕的告警类型包括：

- `HighCPUUsage`
- `HighMemoryUsage`
- `HighDiskUsage`
- `ServiceUnavailable`
- `SlowResponse`

更准确的总结是：

> Prometheus 在这个项目里承担的是 AIOps workflow 的告警入口，告诉 Agent 当前有哪些异常正在发生，后续再结合日志和知识库做分析。

## 16. 这些工具到底能解决什么问题？

可以把它们理解成三类核心能力：

### 16.1 经验类问题

由知识库工具解决。

- 工具：`queryInternalDocs`
- 问题：内部 SOP、历史经验、运维手册去哪查

### 16.2 状态类问题

由 Prometheus 和指标类工具解决。

- 工具：`queryPrometheusAlerts`、MCP 的 `QueryMetric`
- 问题：当前有哪些告警、指标异常点在哪里

### 16.3 证据类问题

由日志工具解决。

- 工具：`queryLogs`、MCP 的 `SearchLog`
- 问题：现场日志到底报了什么错、上下文是什么

所以这个项目的本质不是“大模型会聊天”，而是：

> 让模型从“只会生成文本”变成“能基于知识、状态和证据做回答与分析”。

## 17. 一组最稳的总括口径

如果需要把上面这些内容压成几句话，可以使用下面这套口径：

> 当前项目里普通问答使用单个 ReAct Agent，它和知识库之间不是 Agent 通信，而是通过 `queryInternalDocs` 这类 tool 完成同步调用。AIOps 场景则使用 Supervisor、Planner、Executor 组成的外层 workflow orchestration，其中 Planner 侧重知识和告警理解，Executor 侧重日志与指标取证，两者通过共享状态 `planner_plan` 与 `executor_feedback` 间接通信。  
> 项目里的 MCP 也是 Client 侧集成，不是自己实现 MCP Server；远端能力通过 Spring AI MCP Client 发现后，作为 ToolCallback 挂载到 Agent 上。知识入库本身不是 Agent，而是异步索引 pipeline；知识检索是 tool。Prometheus 提供告警入口，日志工具提供现场证据，知识库提供 SOP 和经验，三者一起支撑 Agent 的分析链路。
