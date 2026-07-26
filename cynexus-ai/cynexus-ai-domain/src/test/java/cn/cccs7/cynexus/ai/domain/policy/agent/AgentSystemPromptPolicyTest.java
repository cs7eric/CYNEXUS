package cn.cccs7.cynexus.ai.domain.policy.agent;

import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionLimits;
import org.junit.jupiter.api.Test;

class AgentSystemPromptPolicyTest {

    @Test
    void rendersFallbackTemplateVariables() {
        String prompt = AgentSystemPromptPolicy.render(
                null,
                "2026-07-24",
                3,
                1,
                false,
                AgentExecutionLimits.defaults());
        assertTrue(prompt.contains("2026-07-24"));
        assertTrue(prompt.contains("本轮工具预算 3 次"));
        assertTrue(prompt.contains("已使用 1 次"));
        assertTrue(prompt.contains("接近预算时优先收敛作答"));
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
}
