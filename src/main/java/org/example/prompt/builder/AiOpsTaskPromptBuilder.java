package org.example.prompt.builder;

import org.springframework.stereotype.Component;

@Component
public class AiOpsTaskPromptBuilder implements PromptBuilder<NoPromptContext> {

    @Override
    public String build(NoPromptContext context) {
        return "你是企业级 SRE，接到了自动化告警排查任务。请结合工具调用，执行**规划→执行→再规划**的闭环，并最终按照固定模板输出《告警分析报告》。禁止编造虚假数据，如连续多次查询失败需诚实反馈无法完成的原因。";
    }
}
