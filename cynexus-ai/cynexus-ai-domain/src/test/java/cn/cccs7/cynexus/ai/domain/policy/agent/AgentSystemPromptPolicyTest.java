package cn.cccs7.cynexus.ai.domain.policy.agent;

import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.cccs7.cynexus.ai.domain.entity.AiPromptBO;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionLimits;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgentSystemPromptPolicyTest {

    @Test
    void fallsBackWhenPromptMissing() {
        String prompt = AgentSystemPromptPolicy.resolve(
                null,
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
    void prefersBoundPromptId() {
        AiPromptRepository repository = new AiPromptRepository() {
            @Override
            public List<AiPromptBO> list(String keyword, String scenarioCode, String status) {
                return List.of(AiPromptBO.builder()
                        .promptKey(AgentSystemPromptPolicy.PROMPT_KEY)
                        .status("active")
                        .systemPrompt("list template {{current_date}}")
                        .build());
            }

            @Override
            public AiPromptBO findById(Long id) {
                if (Long.valueOf(99L).equals(id)) {
                    return AiPromptBO.builder()
                            .id(99L)
                            .promptKey(AgentSystemPromptPolicy.PROMPT_KEY)
                            .status("active")
                            .systemPrompt("bound template 日期={{current_date}} 预算={{tool_budget}} {{tool_budget_guidance}}")
                            .build();
                }
                return null;
            }

            @Override
            public Long create(AiPromptBO prompt) { return 1L; }

            @Override
            public boolean update(AiPromptBO prompt) { return true; }

            @Override
            public boolean delete(Long id) { return true; }
        };

        String prompt = AgentSystemPromptPolicy.resolve(
                repository,
                99L,
                "2026-07-24",
                2,
                2,
                true,
                AgentExecutionLimits.defaults());
        assertTrue(prompt.startsWith("bound template"));
        assertTrue(prompt.contains("日期=2026-07-24"));
        assertTrue(prompt.contains("预算=2"));
        assertTrue(prompt.contains("禁止再调用任何工具"));
    }
}
