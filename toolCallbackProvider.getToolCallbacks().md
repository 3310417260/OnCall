io.modelcontextprotocol.client.McpAsyncClient@3e611510 ,"Tool[name=ConvertTimeStringToTimestamp, title=null, description=将时间字符串转换为毫秒或秒级时间戳。常用于为 SearchLog、DescribeLogHistogram 等工具的 From/To 参数准备时间戳。, inputSchema=JsonSchema[type=object, properties={timeFormat={default=YYYY-MM-DDTHH:mm: ss.sssZ, description=时间格式，如 YYYY-MM-DDTHH:mm: ss.sssZ。默认 ISO 8601 格式。若 timeString 非 ISO 8601 格式则必须提供。, type=string}, timeString={description=要转换的时间字符串，如 2026-01-07T02:34:53.623Z。强烈建议使用 ISO 8601 格式（YYYY-MM-DDTHH:mm: ss.sssZ）。若非 ISO 8601 格式，必须同时提供 timeFormat 参数。, type=string}, timeZone={default=UTC, description=时区，如 Asia/Shanghai。若 timeString 不含时区偏移信息则必须提供。, type=string}, unit={default=milliseconds, description=返回时间戳的单位。""milliseconds""（毫秒，默认）或 ""seconds""（秒）。, enum=[milliseconds, seconds], type=string}}, required=[timeString], additionalProperties=null, defs=null, definitions=null], outputSchema=null, annotations=ToolAnnotations[title=null, readOnlyHint=null, destructiveHint=null, idempotentHint=null, openWorldHint=null, returnDirect=null], meta=null]",ConvertTimeStringToTimestamp,org.springframework.ai.mcp.ToolContextToMcpMetaConverter $$Lambda$1213/0x00000003017d5328@6343e327
io.modelcontextprotocol.client.McpAsyncClient@3e611510,"Tool[name=ConvertTimestampToTimeString, title=null, description=将时间戳转换为时间字符串。不传 timestamp 参数时返回当前时间，常用于获取当前时间后计算 SearchLog 等工具所需的时间范围。, inputSchema=JsonSchema[type=object, properties={timeFormat={default=YYYY-MM-DDTHH:mm:ss.sssZ, description=输出时间格式，如 YYYY-MM-DDTHH:mm:ss.sssZ。默认 ISO 8601 格式。, type=string}, timeZone={default=UTC, description=输出时区，如 Asia/Shanghai。默认使用系统时区。, type=string}, timestamp={description=要转换的时间戳。不传则返回当前时间。单位由 unit 参数决定。, type=number}, unit={default=milliseconds, description=输入时间戳的单位。""milliseconds""（毫秒，默认）或 ""seconds""（秒）。, enum=[milliseconds, seconds], type=string}}, required=null, additionalProperties=null, defs=null, definitions=null], outputSchema=null, annotations=ToolAnnotations[title=null, readOnlyHint=null, destructiveHint=null, idempotentHint=null, openWorldHint=null, returnDirect=null], meta=null]",ConvertTimestampToTimeString,org.springframework.ai.mcp.ToolContextToMcpMetaConverter$$Lambda$1213/0x00000003017d5328@6343e327
io.modelcontextprotocol.client.McpAsyncClient@3e611510,"Tool[name=DescribeAlarmNotices, title=null, description=获取 CLS 通知渠道组列表。查询指定地域下的通知渠道组列表，通知渠道组用于配置告警通知的接收方式和接收人，包括邮件、短信、电话、企业微信等。

支持的过滤条件（Filters 参数）：
- name: 按通知渠道组名称过滤
- alarmNoticeId: 按通知渠道组 ID 过滤

返回信息包含：AlarmNoticeId、Name、NoticeReceivers、WebCallbacks、CreateTime、UpdateTime 等。, inputSchema=JsonSchema[type=object, properties={Filters={description=过滤条件列表，每个过滤条件包含 Key 和 Values 字段。, items={additionalProperties=false, properties={Key={description=过滤条件的键, type=string}, Values={description=过滤条件的值列表, items={type=string}, type=array}}, required=[Key, Values], type=object}, type=array}, Limit={default=20, description=单页返回的数量，最大 100，默认 20, type=number}, Offset={default=0, description=分页偏移量，从 0 开始，默认为 0, type=number}, Region={description=地域信息，必选，如：ap-guangzhou。, type=string}}, required=[Region], additionalProperties=null, defs=null, definitions=null], outputSchema=null, annotations=ToolAnnotations[title=null, readOnlyHint=null, destructiveHint=null, idempotentHint=null, openWorldHint=null, returnDirect=null], meta=null]",DescribeAlarmNotices,org.springframework.ai.mcp.ToolContextToMcpMetaConverter$$Lambda$1213/0x00000003017d5328@6343e327
io.modelcontextprotocol.client.McpAsyncClient@3e611510,"Tool[name=DescribeAlarmShields, title=null, description=获取 CLS 告警屏蔽规则列表。查询指定通知渠道组下的告警屏蔽规则，屏蔽规则用于在特定时间段内屏蔽告警通知，避免维护期间产生大量告警噪音。

应用场景：
1. 查看某个通知渠道组下配置的所有屏蔽规则
2. 查询当前生效的屏蔽规则（按状态过滤）
3. 审计和管理告警屏蔽配置

支持的过滤条件（Filters 参数）：
- shieldId: 按屏蔽规则 ID 过滤，如 [{Key: 'shieldId', Values: ['shield-xxx']}]
- name: 按屏蔽规则名称过滤，如 [{Key: 'name', Values: ['维护屏蔽']}]
- status: 按状态过滤（enabled=启用，disabled=禁用），如 [{Key: 'status', Values: ['enabled']}], inputSchema=JsonSchema[type=object, properties={AlarmNoticeId={description=通知渠道组 ID，必填参数。可通过 DescribeAlarmNotices 工具获取。, type=string}, Filters={description=过滤条件列表，每个过滤条件包含 Key 和 Values 字段。, items={additionalProperties=false, properties={Key={description=过滤条件的键, type=string}, Values={description=过滤条件的值列表, items={type=string}, type=array}}, required=[Key, Values], type=object}, type=array}, Limit={default=20, description=单页返回的数量，最大 100，默认 20, type=number}, Offset={default=0, description=分页偏移量，从 0 开始，默认为 0, type=number}, Region={description=地域信息，必选，如：ap-guangzhou。, type=string}}, required=[Region, AlarmNoticeId], additionalProperties=null, defs=null, definitions=null], outputSchema=null, annotations=ToolAnnotations[title=null, readOnlyHint=null, destructiveHint=null, idempotentHint=null, openWorldHint=null, returnDirect=null], meta=null]",DescribeAlarmShields,org.springframework.ai.mcp.ToolContextToMcpMetaConverter$$Lambda$1213/0x00000003017d5328@6343e327
io.modelcontextprotocol.client.McpAsyncClient@3e611510,"Tool[name=DescribeAlarms, title=null, description=获取 CLS 告警策略列表。查询指定地域的告警策略列表，支持按告警策略启用状态等条件过滤和分页。

支持的过滤条件（Filters 参数）：
- name: 按告警策略名称过滤
- alarmId: 按告警策略 ID 过滤
- topicId: 按监控对象的日志主题 ID 过滤
- enable: 按启用状态过滤（1=启用，0=禁用），如 [{Key: 'enable', Values: ['1']}], inputSchema=JsonSchema[type=object, properties={Filters={description=过滤条件列表，每个过滤条件包含 Key 和 Values 字段。, items={additionalProperties=false, properties={Key={description=过滤条件的键, type=string}, Values={description=过滤条件的值列表, items={type=string}, type=array}}, required=[Key, Values], type=object}, type=array}, Limit={default=20, description=单页返回的数量，最大 100，默认 20, type=number}, Offset={default=0, description=分页偏移量，从 0 开始，默认为 0, type=number}, Region={description=地域信息，必选，如：ap-guangzhou。, type=string}}, required=[Region], additionalProperties=null, defs=null, definitions=null], outputSchema=null, annotations=ToolAnnotations[title=null, readOnlyHint=null, destructiveHint=null, idempotentHint=null, openWorldHint=null, returnDirect=null], meta=null]",DescribeAlarms,org.springframework.ai.mcp.ToolContextToMcpMetaConverter$$Lambda$1213/0x00000003017d5328@6343e327
io.modelcontextprotocol.client.McpAsyncClient@3e611510,"Tool[name=DescribeAlertRecordHistory, title=null, description=获取 CLS 告警历史记录。查询指定时间范围内的告警历史记录，包含告警触发、恢复等事件的详细信息，用于分析告警趋势和排查告警问题。

支持的过滤条件（Filters 参数）：
- alarmId: 按告警策略 ID 过滤
- alarmName: 按告警策略名称过滤
- topicId: 按日志主题 ID 过滤
- status: 按告警状态过滤（0-未恢复，1-已恢复）

返回信息包含：TotalCount（总数）、Records 列表（每条记录含 RecordId、AlarmId、AlarmName、TopicId、TopicName、Region、Trigger、TriggerCount、AlarmLevel、Status、CreateTime、Duration、NotifyStatus）。, inputSchema=JsonSchema[type=object, properties={Filters={description=过滤条件列表，每个过滤条件包含 Key 和 Values 字段。, items={additionalProperties=false, properties={Key={description=过滤条件的键, type=string}, Values={description=过滤条件的值列表, items={type=string}, type=array}}, required=[Key, Values], type=object}, type=array}, From={description=查询起始时间，Unix时间戳（毫秒单位）。应当先调用 ConvertTimestampToTimeString 工具获取当前时间(不传timestamp参数就是获取当前时间)，基于时间字符串计算好From、To参数后，再调用 ConvertTimeStringToTimestamp 工具获取时间戳。To减去From的时间范围建议不要过大，建议默认近15分钟，否则会导致返回的数据过多，影响性能。, type=number}, Limit={default=20, description=单页返回的数量，最大 100，默认 20, type=number}, Offset={default=0, description=分页偏移量，从 0 开始，默认为 0, type=number}, Region={description=地域信息，必选，如：ap-guangzhou。, type=string}, To={description=查询结束时间，Unix时间戳（毫秒单位）。应当先调用 ConvertTimestampToTimeString 工具获取当前时间(不传timestamp参数就是获取当前时间)，基于时间字符串计算好From、To参数后，再调用 ConvertTimeStringToTimestamp 工具获取时间戳。To减去From的时间范围建议不要过大，建议默认近15分钟，否则会导致返回的数据过多，影响性能。, type=number}}, required=[Region, From, To], additionalProperties=null, defs=null, definitions=null], outputSchema=null, annotations=ToolAnnotations[title=null, readOnlyHint=null, destructiveHint=null, idempotentHint=null, openWorldHint=null, returnDirect=null], meta=null]",DescribeAlertRecordHistory,org.springframework.ai.mcp.ToolContextToMcpMetaConverter$$Lambda$1213/0x00000003017d5328@6343e327
io.modelcontextprotocol.client.McpAsyncClient@3e611510,"Tool[name=DescribeIndex, title=null, description=获取 CLS 日志主题索引配置。查询指定日志主题的索引配置信息，包括键值索引、元数据索引（TAG）和全文索引的详细配置。
索引配置决定了日志哪些字段可以被搜索和分析，是日志检索和分析的基础配置。

使用场景：生成检索语句时，推荐优先使用 TextToSearchLogQuery 工具（自动适配索引配置）。仅在需要手写 CQL 查询时，才使用本工具了解日志主题有哪些可检索字段及其类型。

返回字段包含：字段名及其 type（类型）、sql_flag（是否支持 SQL 分析）、description（字段描述）；__TAG__（元数据索引）；__FULLTEXT__（全文索引）。, inputSchema=JsonSchema[type=object, properties={Region={description=地域信息，必选，如：ap-guangzhou。, type=string}, TopicId={description=日志主题 ID，需要查询索引配置的日志主题标识符。, type=string}}, required=[Region, TopicId], additionalProperties=null, defs=null, definitions=null], outputSchema=null, annotations=ToolAnnotations[title=null, readOnlyHint=null, destructiveHint=null, idempotentHint=null, openWorldHint=null, returnDirect=null], meta=null]",DescribeIndex,org.springframework.ai.mcp.ToolContextToMcpMetaConverter$$Lambda$1213/0x00000003017d5328@6343e327
io.modelcontextprotocol.client.McpAsyncClient@3e611510,"Tool[name=DescribeLogContext, title=null, description=获取指定日志的上下文内容（前后 N 条日志）。用于在定位到某条异常日志后，查看其前后的日志以分析问题根因。

前置条件：需先使用 SearchLog 工具检索到目标日志，从返回结果的 Results 中获取 Time、PkgId、PkgLogId 三个必填参数。, inputSchema=JsonSchema[type=object, properties={From={description=查询起始时间，Unix时间戳（毫秒单位），可选。应当先调用 ConvertTimestampToTimeString 工具获取当前时间(不传timestamp参数就是获取当前时间)，基于时间字符串计算好From、To参数后，再调用 ConvertTimeStringToTimestamp 工具获取时间戳。To减去From的时间范围建议不要过大，建议默认近15分钟，否则会导致返回的数据过多，影响性能。, type=number}, NextLogs={default=10, description=向后获取的日志条数，默认 10。, type=number}, PkgId={description=日志上报请求包的ID。通过 SearchLog 工具检索原始日志时，Results 结构体中会返回 PkgId 字段。, type=string}, PkgLogId={description=请求包内日志的ID。通过 SearchLog 工具检索原始日志时，Results 结构体中会返回 PkgLogId 字段。, type=number}, PrevLogs={default=10, description=向前获取的日志条数，默认 10。, type=number}, Query={description=检索语句，对日志上下文进行过滤，不支持SQL语句。, type=string}, Region={description=地域信息，必选，如：ap-guangzhou。, type=string}, Time={description=日志时间，单位ms。通过 SearchLog 工具检索原始日志时，Results 结构体中会返回 Time 字段。, type=number}, To={description=查询结束时间，Unix时间戳（毫秒单位），可选。应当先调用 ConvertTimestampToTimeString 工具获取当前时间(不传timestamp参数就是获取当前时间)，基于时间字符串计算好From、To参数后，再调用 ConvertTimeStringToTimestamp 工具获取时间戳。To减去From的时间范围建议不要过大，建议默认近15分钟，否则会导致返回的数据过多，影响性能。, type=number}, TopicId={description=要检索分析的日志主题ID，仅能指定一个日志主题, type=string}}, required=[Region, TopicId, Time, PkgId, PkgLogId], additionalProperties=null, defs=null, definitions=null], outputSchema=null, annotations=ToolAnnotations[title=null, readOnlyHint=null, destructiveHint=null, idempotentHint=null, openWorldHint=null, returnDirect=null], meta=null]",DescribeLogContext,org.springframework.ai.mcp.ToolContextToMcpMetaConverter$$Lambda$1213/0x00000003017d5328@6343e327
io.modelcontextprotocol.client.McpAsyncClient@3e611510,"Tool[name=DescribeLogHistogram, title=null, description=获取 CLS 日志直方图数据。查询指定日志主题在指定时间范围内的日志分布直方图，统计各时间段内匹配查询条件的日志数量。

重要：本工具仅返回时间（BTime）和计数（Count）两个维度，无法做多字段聚合分析。
- 如果日志主题支持 SQL 分析（标准存储），应优先使用 SearchLog 工具通过管道符 | 进行时间分组统计，功能更强大
- 本工具适用于不支持 SQL 分析的日志主题（如低频存储主题），或只需快速查看日志量时间趋势的场景

Query 参数使用 CQL 语法，建议先使用 TextToSearchLogQuery 生成。

返回信息包含：Interval（时间间隔）、TotalCount（总日志条数）、HistogramInfos（各时间段的 BTime 起始时间和 Count 日志计数）。, inputSchema=JsonSchema[type=object, properties={From={description=查询起始时间，Unix时间戳（毫秒单位）。应当先调用 ConvertTimestampToTimeString 工具获取当前时间(不传timestamp参数就是获取当前时间)，基于时间字符串计算好From、To参数后，再调用 ConvertTimeStringToTimestamp 工具获取时间戳。To减去From的时间范围建议不要过大，建议默认近15分钟，否则会导致返回的数据过多，影响性能。, type=number}, Interval={description=统计时间间隔，单位毫秒。常用值：60000（1分钟）、300000（5分钟）、600000（10分钟）。不传则系统自动计算。, type=number}, Query={description=CQL 查询语句，用于过滤日志。使用 * 查询所有日志。, type=string}, Region={description=地域信息，必选，如：ap-guangzhou。, type=string}, To={description=查询结束时间，Unix时间戳（毫秒单位）。应当先调用 ConvertTimestampToTimeString 工具获取当前时间(不传timestamp参数就是获取当前时间)，基于时间字符串计算好From、To参数后，再调用 ConvertTimeStringToTimestamp 工具获取时间戳。To减去From的时间范围建议不要过大，建议默认近15分钟，否则会导致返回的数据过多，影响性能。, type=number}, TopicId={description=日志主题 ID，需要查询直方图的日志主题标识符。, type=string}}, required=[Region, TopicId, From, To, Query], additionalProperties=null, defs=null, definitions=null], outputSchema=null, annotations=ToolAnnotations[title=null, readOnlyHint=null, destructiveHint=null, idempotentHint=null, openWorldHint=null, returnDirect=null], meta=null]",DescribeLogHistogram,org.springframework.ai.mcp.ToolContextToMcpMetaConverter$$Lambda$1213/0x00000003017d5328@6343e327
io.modelcontextprotocol.client.McpAsyncClient@3e611510,"Tool[name=DescribeNoticeContents, title=null, description=获取 CLS 通知内容模板列表。查询指定地域下的通知内容模板，模板定义告警触发和恢复时的消息内容，支持邮件、短信、企业微信、Webhook 等渠道。

支持的过滤条件（Filters 参数）：
- name: 按模板名称过滤
- noticeContentId: 按模板 ID 过滤

返回信息包含：NoticeContentId、Name、Type、NoticeContents（各渠道内容配置）、CreateTime、UpdateTime 等。, inputSchema=JsonSchema[type=object, properties={Filters={description=过滤条件列表，每个过滤条件包含 Key 和 Values 字段。, items={additionalProperties=false, properties={Key={description=过滤条件的键, type=string}, Values={description=过滤条件的值列表, items={type=string}, type=array}}, required=[Key, Values], type=object}, type=array}, Limit={default=20, description=单页返回的数量，最大 100，默认 20, type=number}, Offset={default=0, description=分页偏移量，从 0 开始，默认为 0, type=number}, Region={description=地域信息，必选，如：ap-guangzhou。, type=string}}, required=[Region], additionalProperties=null, defs=null, definitions=null], outputSchema=null, annotations=ToolAnnotations[title=null, readOnlyHint=null, destructiveHint=null, idempotentHint=null, openWorldHint=null, returnDirect=null], meta=null]",DescribeNoticeContents,org.springframework.ai.mcp.ToolContextToMcpMetaConverter$$Lambda$1213/0x00000003017d5328@6343e327
io.modelcontextprotocol.client.McpAsyncClient@3e611510,"Tool[name=DescribeWebCallbacks, title=null, description=获取 CLS 告警回调配置列表。查询指定地域下的 Webhook 回调配置，用于在告警触发或恢复时向指定 URL 发送 HTTP 通知，常用于与第三方监控系统或自动化运维系统集成。

支持的过滤条件（Filters 参数）：
- name: 按回调配置名称过滤
- callbackId: 按回调配置 ID 过滤

返回信息包含：CallbackId、Name、Url、Method、Headers、Body、CreateTime、UpdateTime 等。, inputSchema=JsonSchema[type=object, properties={Filters={description=过滤条件列表，每个过滤条件包含 Key 和 Values 字段。, items={additionalProperties=false, properties={Key={description=过滤条件的键, type=string}, Values={description=过滤条件的值列表, items={type=string}, type=array}}, required=[Key, Values], type=object}, type=array}, Limit={default=20, description=单页返回的数量，最大 100，默认 20, type=number}, Offset={default=0, description=分页偏移量，从 0 开始，默认为 0, type=number}, Region={description=地域信息，必选，如：ap-guangzhou。, type=string}}, required=[Region], additionalProperties=null, defs=null, definitions=null], outputSchema=null, annotations=ToolAnnotations[title=null, readOnlyHint=null, destructiveHint=null, idempotentHint=null, openWorldHint=null, returnDirect=null], meta=null]",DescribeWebCallbacks,org.springframework.ai.mcp.ToolContextToMcpMetaConverter$$Lambda$1213/0x00000003017d5328@6343e327
io.modelcontextprotocol.client.McpAsyncClient@3e611510,"Tool[name=GetAlarmDetail, title=null, description=通过告警详情 URL 获取 CLS 告警的详细信息。从告警通知中的 URL 提取并解析告警信息，支持短链接和长链接格式。

支持的 URL 格式：
1. 短链接：https://alarm.cls.tencentcs.com/WeNZ5sSP
2. 短链接：https://mc.tencent.com/xxx
3. 长链接：https://ap-guangzhou-monitor.cls.tencentcs.com/cls_no_login?action=GetAlertDetailPage#/alert?RecordId=xxx

返回 Markdown 格式的告警详细信息，包含：
- 告警基本信息（名称、ID、地域）
- 告警详细数据（监控对象、触发时间、持续时间、触发条件）
- 触发语句（CQL 查询）
- 多维分析结果（字段分布、查询结果表格）

应用场景：直接粘贴告警通知中的 URL 即可获取完整告警信息，用于快速排查和分析。, inputSchema=JsonSchema[type=object, properties={AlarmDetailUrl={description=告警详情 URL，支持短链接和长链接格式。, type=string}}, required=[AlarmDetailUrl], additionalProperties=null, defs=null, definitions=null], outputSchema=null, annotations=ToolAnnotations[title=null, readOnlyHint=null, destructiveHint=null, idempotentHint=null, openWorldHint=null, returnDirect=null], meta=null]",GetAlarmDetail,org.springframework.ai.mcp.ToolContextToMcpMetaConverter$$Lambda$1213/0x00000003017d5328@6343e327
io.modelcontextprotocol.client.McpAsyncClient@3e611510,"Tool[name=GetAlarmLog, title=null, description=获取 CLS 告警执行详情日志。查询指定时间范围内的告警策略执行详情，包括执行时间、结果、触发的日志内容等。

查询语法说明：
- * : 查询所有告警策略的执行详情
- AlarmId:""alarm-xxx"" : 查询指定告警策略的执行详情
- AlarmName:""告警名称"" : 按告警名称查询
- 组合查询：AlarmId:""alarm-xxx"" AND Status:""success""

分页说明：首次不传 Context；若返回 ListOver 为 false，用返回的 Context 获取后续数据。

返回信息包含：Results 列表（每条含 AlarmId、AlarmName、TopicId、TopicName、Trigger、TriggerCount、AlarmLevel、Status、CreateTime、Duration、NotifyStatus、Content）、Context（分页标识）、ListOver（是否查询完毕）。, inputSchema=JsonSchema[type=object, properties={Context={description=上下文标识符，用于分页查询获取后续数据。, type=string}, From={description=查询起始时间，Unix时间戳（毫秒单位）。应当先调用 ConvertTimestampToTimeString 工具获取当前时间(不传timestamp参数就是获取当前时间)，基于时间字符串计算好From、To参数后，再调用 ConvertTimeStringToTimestamp 工具获取时间戳。To减去From的时间范围建议不要过大，建议默认近15分钟，否则会导致返回的数据过多，影响性能。, type=number}, Limit={default=100, description=单次返回条数，最大 1000，默认 100。, type=number}, Query={description=查询过滤条件，支持 CLS 查询语法，如 * 表示查询所有。, type=string}, Region={description=地域信息，必选，如：ap-guangzhou。, type=string}, Sort={default=desc, description=排序方式：asc（升序）、desc（降序），默认 desc。, type=string}, To={description=查询结束时间，Unix时间戳（毫秒单位）。应当先调用 ConvertTimestampToTimeString 工具获取当前时间(不传timestamp参数就是获取当前时间)，基于时间字符串计算好From、To参数后，再调用 ConvertTimeStringToTimestamp 工具获取时间戳。To减去From的时间范围建议不要过大，建议默认近15分钟，否则会导致返回的数据过多，影响性能。, type=number}}, required=[Region, From, To, Query], additionalProperties=null, defs=null, definitions=null], outputSchema=null, annotations=ToolAnnotations[title=null, readOnlyHint=null, destructiveHint=null, idempotentHint=null, openWorldHint=null, returnDirect=null], meta=null]",GetAlarmLog,org.springframework.ai.mcp.ToolContextToMcpMetaConverter$$Lambda$1213/0x00000003017d5328@6343e327
io.modelcontextprotocol.client.McpAsyncClient@3e611510,"Tool[name=GetRegionCodeByName, title=null, description=按地域名称搜索腾讯云地域参数代码（如""广州""→""ap-guangzhou""），支持中文和英文名称模糊匹配。, inputSchema=JsonSchema[type=object, properties={language={default=zh-CN, description=搜索文本的语言，""zh-CN""（中文，默认）或 ""en-US""（英文）, type=string}, searchText={description=地域名称，如 Hong Kong 或 广州, type=string}}, required=[searchText], additionalProperties=null, defs=null, definitions=null], outputSchema=null, annotations=ToolAnnotations[title=null, readOnlyHint=null, destructiveHint=null, idempotentHint=null, openWorldHint=null, returnDirect=null], meta=null]",GetRegionCodeByName,org.springframework.ai.mcp.ToolContextToMcpMetaConverter$$Lambda$1213/0x00000003017d5328@6343e327
io.modelcontextprotocol.client.McpAsyncClient@3e611510,"Tool[name=GetTopicInfoByName, title=null, description=按名称搜索日志主题或指标主题信息，返回主题 ID、名称、保留周期等信息。, inputSchema=JsonSchema[type=object, properties={Region={description=地域信息，必选，如：ap-guangzhou。, type=string}, bizType={default=0, description=主题类型。0：日志主题（默认值）；1：指标主题。查询指标主题时需传入 1。, type=number}, limit={default=20, description=单页返回数量，默认 20, type=number}, offset={default=0, description=分页偏移量，默认 0, type=number}, preciseSearch={default=false, description=是否精确匹配（true）或模糊匹配（false），默认 false。推荐使用模糊匹配。, type=boolean}, searchText={description=搜索日志主题名称。不传则返回所有主题。, type=string}}, required=[Region], additionalProperties=null, defs=null, definitions=null], outputSchema=null, annotations=ToolAnnotations[title=null, readOnlyHint=null, destructiveHint=null, idempotentHint=null, openWorldHint=null, returnDirect=null], meta=null]",GetTopicInfoByName,org.springframework.ai.mcp.ToolContextToMcpMetaConverter$$Lambda$1213/0x00000003017d5328@6343e327
io.modelcontextprotocol.client.McpAsyncClient@3e611510,"Tool[name=QueryMetric, title=null, description=针对指标主题，查询指定时刻指标的最新值（瞬时查询）。使用 PromQL 语法对指标主题中的数据进行查询。注意：若该时刻向前推5分钟内均无指标数据，则无相应查询结果。

PromQL 语法示例：
- 简单查询：ETLProcessingTraffic
- 速率计算：rate(http_requests_total[5m])
- 聚合查询：sum(cpu_usage) by (instance)
- 参考文档：https://cloud.tencent.com/document/product/614/90334, inputSchema=JsonSchema[type=object, properties={Query={description=查询语句，必须使用 PromQL 语法，如 access_evaluation_duration_bucket。注意：本参数仅接受 PromQL，严禁传入 CQL/SQL 语法（例如 *、SELECT、WHERE 等日志检索语句），否则会报错。参考文档：https://cloud.tencent.com/document/product/614/90334, type=string}, Region={description=地域信息，必选，如：ap-guangzhou。, type=string}, Time={description=查询时间，秒级 Unix 时间戳。为空时代表当前时间戳。如需指定时间，应当先调用 ConvertTimestampToTimeString 工具获取当前时间（不传 timestamp 参数即获取当前时间），基于时间字符串计算好目标时间后，再调用 ConvertTimeStringToTimestamp 工具并指定 unit 为 ""seconds"" 直接获取秒级时间戳传入。, type=number}, TopicId={description=指标主题ID，通过 GetTopicInfoByName 工具并指定 bizType 为 1 获取指标主题 ID。, type=string}}, required=[Region, TopicId, Query], additionalProperties=null, defs=null, definitions=null], outputSchema=null, annotations=ToolAnnotations[title=null, readOnlyHint=null, destructiveHint=null, idempotentHint=null, openWorldHint=null, returnDirect=null], meta=null]",QueryMetric,org.springframework.ai.mcp.ToolContextToMcpMetaConverter$$Lambda$1213/0x00000003017d5328@6343e327
io.modelcontextprotocol.client.McpAsyncClient@3e611510,"Tool[name=QueryRangeMetric, title=null, description=针对指标主题，查询指定时间范围内指标的变化趋势（范围查询）。使用 PromQL 语法对指标主题中的数据进行时序查询，返回区间内的时序数据。

step 参数建议：根据查询时间范围合理设置数据点密度
- 1 小时内：step=60（每分钟）
- 1 天内：step=300（每 5 分钟）
- 1 周内：step=3600（每小时）
- 1 月内：step=86400（每天）, inputSchema=JsonSchema[type=object, properties={End={description=查询结束时间，秒级 Unix 时间戳。应当先调用 ConvertTimestampToTimeString 工具获取当前时间（不传 timestamp 参数即获取当前时间），基于时间字符串计算好目标时间后，再调用 ConvertTimeStringToTimestamp 工具并指定 unit 为 ""seconds"" 直接获取秒级时间戳传入。End减去Start的时间范围建议不要过大，建议默认近15分钟，否则会导致返回过多数据，影响性能。, type=number}, Query={description=查询语句，必须使用 PromQL 语法，如 access_evaluation_duration_bucket。注意：本参数仅接受 PromQL，严禁传入 CQL/SQL 语法（例如 *、SELECT、WHERE 等日志检索语句），否则会报错。参考文档：https://cloud.tencent.com/document/product/614/90334, type=string}, Region={description=地域信息，必选，如：ap-guangzhou。, type=string}, Start={description=查询起始时间，秒级 Unix 时间戳。应当先调用 ConvertTimestampToTimeString 工具获取当前时间（不传 timestamp 参数即获取当前时间），基于时间字符串计算好目标时间后，再调用 ConvertTimeStringToTimestamp 工具并指定 unit 为 ""seconds"" 直接获取秒级时间戳传入。End减去Start的时间范围建议不要过大，建议默认近15分钟，否则会导致返回过多数据，影响性能。, type=number}, Step={description=查询时间间隔，单位秒。例如 60 表示每 60 秒一个数据点。, type=number}, TopicId={description=指标主题ID，通过 GetTopicInfoByName 工具并指定 bizType 为 1 获取指标主题 ID。, type=string}}, required=[Region, TopicId, Query, Start, End, Step], additionalProperties=null, defs=null, definitions=null], outputSchema=null, annotations=ToolAnnotations[title=null, readOnlyHint=null, destructiveHint=null, idempotentHint=null, openWorldHint=null, returnDirect=null], meta=null]",QueryRangeMetric,org.springframework.ai.mcp.ToolContextToMcpMetaConverter$$Lambda$1213/0x00000003017d5328@6343e327
io.modelcontextprotocol.client.McpAsyncClient@3e611510,"Tool[name=SearchLog, title=null, description=搜索CLS日志内容。在指定日志主题和时间范围内搜索日志，支持复杂查询语法和统计分析。

重要：务必先使用 TextToSearchLogQuery 工具生成 CQL 查询语句！
TextToSearchLogQuery 能自动适配日志主题索引配置，确保字段名称准确、语法正确。
警告：如果不使用 TextToSearchLogQuery 生成 CQL，直接手写很可能出现字段名称错误、语法不规范等问题导致查询失败。

与 DescribeLogHistogram 的分工：
- SearchLog 支持 SQL 分析（管道符 |），可实现按时间分组统计、多维聚合等复杂分析，功能更强大，优先使用
- DescribeLogHistogram 仅返回时间和计数两个维度，适用于不支持 SQL 分析的日志主题（如低频存储主题）

后续操作：
- 查看某条日志的上下文：使用返回结果中的 PkgId、PkgLogId、Time 调用 DescribeLogContext 工具

CQL（Cloud Query Language）语法说明：
1. 全文检索：直接输入关键词，如 error；多关键词空格分隔默认 OR 关系
2. 键值检索：key:value 格式，如 level:ERROR、status:404
3. 短语检索：双引号包裹，如 name:""john Smith""
4. 模糊检索：* 匹配多字符，? 匹配单字符，如 host:www.test*.com
5. 数值比较：支持 >、>=、<、<=、=，如 status:>400
6. 范围检索：使用比较运算符组合，如 status:>=400 AND status:<500
7. 逻辑运算符：AND、OR、NOT，支持括号组合，如 (level:ERROR OR level:WARNING) AND pid:1234
8. SQL 分析（管道符 |）：
   - 聚合统计：* | SELECT count(*) AS total
   - 分组统计：* | SELECT count(*) AS cnt, level GROUP BY level
   - 排序限制：* | SELECT count(*) AS cnt, host GROUP BY host ORDER BY cnt DESC LIMIT 10
   - 条件过滤：* | SELECT * WHERE response_time > 1000, inputSchema=JsonSchema[type=object, properties={From={description=查询起始时间，Unix时间戳（毫秒单位）。应当先调用 ConvertTimestampToTimeString 工具获取当前时间(不传timestamp参数就是获取当前时间)，基于时间字符串计算好From、To参数后，再调用 ConvertTimeStringToTimestamp 工具获取时间戳。To减去From的时间范围建议不要过大，建议默认近15分钟，否则会导致返回的数据过多，影响性能。, type=number}, Limit={default=10, description=单次查询返回的日志条数，默认为10，最大值为1000, type=number}, Offset={default=0, description=查询原始日志的偏移量，表示从第几行开始返回原始日志，默认为0, type=number}, Query={description=检索分析语句，最大长度为12KB。如果不限定检索条件，可传 * 或 空字符串，可查询所有日志, type=string}, Region={description=地域信息，必选，如：ap-guangzhou。, type=string}, SamplingRate={default=1, description=执行统计分析时是否对原始日志先进行采样，0：自动采样；0～1：按指定采样率采样；1：不采样, type=number}, Sort={default=desc, description=原始日志是否按时间排序返回；可选值：asc(升序)、desc(降序)，默认为desc, type=string}, To={description=查询结束时间，Unix时间戳（毫秒单位）。应当先调用 ConvertTimestampToTimeString 工具获取当前时间(不传timestamp参数就是获取当前时间)，基于时间字符串计算好From、To参数后，再调用 ConvertTimeStringToTimestamp 工具获取时间戳。To减去From的时间范围建议不要过大，建议默认近15分钟，否则会导致返回的数据过多，影响性能。, type=number}, TopicId={description=要检索分析的日志主题ID，仅能指定一个日志主题, type=string}, Topics={description=要检索分析的日志主题列表，最大支持50个日志主题, items={additionalProperties=false, properties={TopicId={description=要检索分析的日志主题ID, type=string}}, type=object}, type=array}}, required=[From, To, Query, Region], additionalProperties=null, defs=null, definitions=null], outputSchema=null, annotations=ToolAnnotations[title=null, readOnlyHint=null, destructiveHint=null, idempotentHint=null, openWorldHint=null, returnDirect=null], meta=null]",SearchLog,org.springframework.ai.mcp.ToolContextToMcpMetaConverter$$Lambda$1213/0x00000003017d5328@6343e327
io.modelcontextprotocol.client.McpAsyncClient@3e611510,"Tool[name=TextToSearchLogQuery, title=null, description=【CQL专家】AI 智能生成 CLS CQL 查询语句。将自然语言描述转换为可直接用于 SearchLog 或 DescribeLogHistogram 的 CQL 语句。

核心优势：
1. 自动适配日志主题索引配置，字段名称 100% 准确
2. 严格遵循 CQL 语法规范，生成的语句保证可执行
3. 查询性能经过优化，执行效率高
4. 支持从简单过滤到复杂聚合的所有查询场景
5. 自动进行语法校验，确保语句正确性

警告：如果不使用本工具生成 CQL，直接手写一定会出现以下问题：
- 字段名称错误，导致查询无结果
- 语法不符合 CQL 规范，导致查询失败
- 统计逻辑错误，导致结果不符合预期

典型应用场景：
- 简单过滤：""查询 ERROR 级别日志"" → level:'error'
- 字段统计：""查看 IP 分布"" → * | SELECT IP, count(*) AS cnt GROUP BY IP ORDER BY cnt DESC
- 复杂聚合：""按小时统计各状态码数量"" → * | SELECT histogram(__TIMESTAMP__, INTERVAL 1 HOUR) AS hour, status_code, count(*) GROUP BY hour, status_code
- 多维分析：""按地域和业务分组，统计错误数>100的"" → level:ERROR | SELECT region, service, count(*) AS error_count GROUP BY region, service HAVING error_count > 100, inputSchema=JsonSchema[type=object, properties={Region={description=地域信息，必选，如：ap-guangzhou。, type=string}, Text={description=用户的自然语言查询描述，支持中文和英文。如：查询日志条数、Get error logs distribution over time。, type=string}, TopicId={description=要检索分析的日志主题ID，仅能指定一个日志主题, type=string}}, required=[Text, Region, TopicId], additionalProperties=null, defs=null, definitions=null], outputSchema=null, annotations=ToolAnnotations[title=null, readOnlyHint=null, destructiveHint=null, idempotentHint=null, openWorldHint=null, returnDirect=null], meta=null]",TextToSearchLogQuery,org.springframework.ai.mcp.ToolContextToMcpMetaConverter$$Lambda$1213/0x00000003017d5328@6343e327
