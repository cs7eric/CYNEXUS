package cn.cccs7.cynexus.ai.domain.service.agent.prompt;

import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.cccs7.cynexus.ai.domain.entity.AiPromptBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.policy.agent.AgentSystemPromptPolicy;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiUsageBindingRepository;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionLimits;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgentSystemPromptDomainServiceTest {

    @Test
    void prefersBoundPromptIdFromUsageBinding() {
        AiPromptRepository prompts = new AiPromptRepository() {
            @Override public List<AiPromptBO> list(String keyword, String scenarioCode, String status) {
                return List.of(AiPromptBO.builder().promptKey(AgentSystemPromptPolicy.PROMPT_KEY).status("active")
                        .systemPrompt("list template {{current_date}}").build());
            }
            @Override public AiPromptBO findById(Long id) {
                if (Long.valueOf(99L).equals(id)) {
                    return AiPromptBO.builder().id(99L).promptKey(AgentSystemPromptPolicy.PROMPT_KEY).status("active")
                            .systemPrompt("bound template 日期={{current_date}} 预算={{tool_budget}} {{tool_budget_guidance}}").build();
                }
                return null;
            }
            @Override public Long create(AiPromptBO prompt) { return 1L; }
            @Override public boolean update(AiPromptBO prompt) { return true; }
            @Override public boolean delete(Long id) { return true; }
        };
        AiUsageBindingRepository bindings = new AiUsageBindingRepository() {
            @Override public List<AiUsageBindingBO> listAll() {
                return List.of(AiUsageBindingBO.builder().capability(AgentSystemPromptPolicy.PROMPT_KEY)
                        .enabled(true).promptId(99L).build());
            }
            @Override public AiUsageBindingBO findById(Long id) { return null; }
            @Override public Long create(AiUsageBindingBO binding) { return 1L; }
            @Override public boolean update(AiUsageBindingBO binding) { return true; }
            @Override public boolean delete(Long id) { return true; }
        };

        String prompt = new AgentSystemPromptDomainService(prompts, bindings)
                .resolve("2026-07-24", 2, 2, true, AgentExecutionLimits.defaults());
        assertTrue(prompt.startsWith("bound template"));
        assertTrue(prompt.contains("日期=2026-07-24"));
        assertTrue(prompt.contains("禁止再调用任何工具"));
    }
}
