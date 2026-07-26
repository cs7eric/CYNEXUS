package cn.cccs7.cynexus.ai.domain.policy.agent;

import static org.assertj.core.api.Assertions.assertThat;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgentPromptCacheKeyPolicyTest {

    private final AiModelBO model = AiModelBO.builder()
            .providerIdentity("openai")
            .modelCode("gpt-5.4-mini")
            .build();

    @Test
    void keepsStablePrefixIndependentOfToolOrderAndSeparatesConversations() {
        AgentToolDefinition first = tool("market.overview", "Read market data.");
        AgentToolDefinition second = tool("kb.search", "Search the knowledge base.");

        var left = AgentPromptCacheKeyPolicy.context(
                "tenant:user", "conversation-1", "stable prompt", model, List.of(first, second));
        var reordered = AgentPromptCacheKeyPolicy.context(
                "tenant:user", "conversation-1", "stable prompt", model, List.of(second, first));
        var otherConversation = AgentPromptCacheKeyPolicy.context(
                "tenant:user", "conversation-2", "stable prompt", model, List.of(first, second));

        assertThat(left.getStablePrefixKey()).isEqualTo(reordered.getStablePrefixKey());
        assertThat(left.getConversationKey()).isEqualTo(reordered.getConversationKey());
        assertThat(left.getConversationKey()).isNotEqualTo(otherConversation.getConversationKey());
        assertThat(left.getStablePrefixKey()).isEqualTo(otherConversation.getStablePrefixKey());
        assertThat(left.getRoutingKey()).isEqualTo(reordered.getRoutingKey());
        assertThat(left.getRoutingKey()).isNotEqualTo(otherConversation.getRoutingKey());
    }

    @Test
    void changesStablePrefixWhenTheTurnToolSetChanges() {
        AgentToolDefinition tool = tool("market.overview", "Read market data.");

        var withTools = AgentPromptCacheKeyPolicy.context(
                "tenant:user", "conversation-1", "stable prompt", model, List.of(tool));
        var withoutTools = AgentPromptCacheKeyPolicy.context(
                "tenant:user", "conversation-1", "stable prompt", model, List.of());

        assertThat(withTools.getStablePrefixKey()).isNotEqualTo(withoutTools.getStablePrefixKey());
        assertThat(withTools.getConversationKey()).isEqualTo(withoutTools.getConversationKey());
        assertThat(withTools.getRoutingKey()).isNotEqualTo(withoutTools.getRoutingKey());
    }

    @Test
    void changesStablePrefixAndRoutingKeyWhenManagedSystemPromptChanges() {
        var first = AgentPromptCacheKeyPolicy.context(
                "tenant:user", "conversation-1", "stable prompt v1", model, List.of());
        var changed = AgentPromptCacheKeyPolicy.context(
                "tenant:user", "conversation-1", "stable prompt v2", model, List.of());

        assertThat(first.getConversationKey()).isEqualTo(changed.getConversationKey());
        assertThat(first.getStablePrefixKey()).isNotEqualTo(changed.getStablePrefixKey());
        assertThat(first.getRoutingKey()).isNotEqualTo(changed.getRoutingKey());
    }

    private AgentToolDefinition tool(String code, String description) {
        return AgentToolDefinition.builder().code(code).description(description)
                .inputSchemaJson("{\"type\":\"object\"}")
                .outputSchemaJson("{\"type\":\"object\"}")
                .guidanceJson("{\"purpose\":\"read\"}").build();
    }
}
