package cn.cccs7.cynexus.ai.domain.policy.agent;

import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionLimits;
import org.junit.jupiter.api.Test;

class AgentSystemPromptPolicyTest {

    @Test
    void keepsFallbackStableAndRuntimeAgnostic() {
        String prompt = AgentSystemPromptPolicy.render(
                null,
                "2026-07-24",
                3,
                1,
                false,
                AgentExecutionLimits.defaults());
        assertTrue(prompt.contains("系统追加的后置运行时上下文"));
        assertTrue(prompt.contains("current_date、tool_budget、used_tools、force_final_answer"));
        assertTrue(!prompt.contains("2026-07-24"));
        assertTrue(!prompt.contains("tool_budget=3"));
    }

    @Test
    void rendersManagedTemplateAndForceFinalGuidance() {
        String prompt = AgentSystemPromptPolicy.render(
                "托管提示词 日期={{current_date}} 预算={{tool_budget}} 已用={{used_tools}} {{tool_budget_guidance}}",
                "2026-07-24",
                2,
                2,
                true,
                AgentExecutionLimits.defaults());
        assertTrue(prompt.startsWith("托管提示词"));
        assertTrue(prompt.contains("日期=2026-07-24"));
        assertTrue(prompt.contains("预算=2"));
        assertTrue(prompt.contains("已用=2"));
        assertTrue(prompt.contains("禁止再调用任何工具"));
    }

    @Test
    void keepsStablePromptIdenticalWhenRuntimeValuesChange() {
        String first = AgentSystemPromptPolicy.renderStable(
                "固定规则 日期={{current_date}} 预算={{tool_budget}} 已用={{used_tools}} {{tool_budget_guidance}}",
                AgentExecutionLimits.defaults());
        String second = AgentSystemPromptPolicy.renderStable(
                "固定规则 日期={{current_date}} 预算={{tool_budget}} 已用={{used_tools}} {{tool_budget_guidance}}",
                AgentExecutionLimits.defaults());

        assertTrue(first.equals(second));
        assertTrue(first.contains("runtime_context.current_date"));
        assertTrue(first.contains("runtime_context.tool_budget"));
        assertTrue(!first.contains("2026-07-24"));
    }

    @Test
    void putsChangingExecutionStateInRuntimeContext() {
        String context = AgentSystemPromptPolicy.runtimeContext(
                "2026-07-24", 2, 1, true, AgentExecutionLimits.defaults());

        assertTrue(context.contains("current_date=2026-07-24"));
        assertTrue(context.contains("tool_budget=2"));
        assertTrue(context.contains("used_tools=1"));
        assertTrue(context.contains("force_final_answer=true"));
        assertTrue(context.contains("禁止再调用任何工具"));
    }

    @Test
    void fallbackMatchesAutonomousWorkContract() {
        String prompt = AgentSystemPromptPolicy.renderStable(null, AgentExecutionLimits.defaults());

        assertTrue(prompt.contains("职责不是只回答问题"));
        assertTrue(prompt.contains("不要在首轮只复述需求、只给计划"));
        assertTrue(prompt.contains("每次工具返回后，判断目标是否已经满足"));
        assertTrue(prompt.contains("不要因为得到第一份结果就提前结束复杂任务"));
        assertTrue(prompt.contains("采用合理默认"));
        assertTrue(prompt.contains("force_final_answer=true 时停止调用工具，不再澄清"));
        assertTrue(prompt.contains("目标完成后再给最终答复"));
    }
}
