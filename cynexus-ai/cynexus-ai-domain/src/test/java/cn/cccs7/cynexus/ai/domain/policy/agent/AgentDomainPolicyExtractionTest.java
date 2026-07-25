package cn.cccs7.cynexus.ai.domain.policy.agent;

import static org.assertj.core.api.Assertions.assertThat;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunStepBO;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelMessage;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgentDomainPolicyExtractionTest {

    @Test
    void normalizesStructuredFinalAnswer() {
        String raw = "{\"type\":\"analysis\",\"conclusion\":\"ok\",\"usedModules\":[\"market\"]}";
        assertThat(AgentFinalAnswerPolicy.normalize(raw)).contains("结论").contains("ok");
    }

    @Test
    void repairsProviderMarkdownBoundariesBeforePersistence() {
        String raw = "##结论**ok**---###总览（事实）|指标|金额|\n|---|---|\n|支出|1|";

        assertThat(AgentFinalAnswerPolicy.normalize(raw))
                .contains("## 结论")
                .contains("**ok**\n\n---")
                .contains("---\n\n### 总览（事实）")
                .contains("|指标|金额|");
    }

    @Test
    void collapsesToolHistoryForProvider() {
        List<AgentModelMessage> history = List.of(
                AgentModelMessage.builder().role("user").content("q").build(),
                AgentModelMessage.builder().role("assistant").content("").toolCallId("x").build(),
                AgentModelMessage.builder().role("tool").content("{}").build(),
                AgentModelMessage.builder().role("assistant").content("final").build());
        List<AgentModelMessage> collapsed = AgentHistoryCollapsePolicy.collapseForProvider(history);
        assertThat(collapsed).hasSize(2);
        assertThat(collapsed.get(1).getContent()).isEqualTo("final");
    }

    @Test
    void composesBudgetAnswerFromToolSteps() {
        String answer = AgentBudgetAnswerPolicy.compose(
                List.of(AgentRunStepBO.builder().toolCode("market.overview").summary("done").status("SUCCEEDED").build()),
                "partial");
        assertThat(answer).contains("工具预算已用尽").contains("market.overview");
    }
}
