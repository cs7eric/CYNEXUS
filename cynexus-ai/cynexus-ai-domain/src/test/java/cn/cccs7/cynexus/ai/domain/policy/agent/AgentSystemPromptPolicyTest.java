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
}
