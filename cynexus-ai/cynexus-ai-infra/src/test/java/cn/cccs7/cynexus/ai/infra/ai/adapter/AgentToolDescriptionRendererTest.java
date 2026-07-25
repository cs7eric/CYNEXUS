package cn.cccs7.cynexus.ai.infra.ai.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AgentToolDescriptionRendererTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void rendersStructuredGuidanceIntoProviderDescription() {
        String rendered = AgentToolDescriptionRenderer.render(objectMapper, "Read market data.", """
                {
                  "purpose": "Read one bounded market snapshot.",
                  "useWhen": ["The user asks for market context."],
                  "avoidWhen": ["The user asks for a write operation."],
                  "parameterNotes": {"tradeDate": "Optional YYYY-MM-DD date."},
                  "examples": [{"arguments": {"tradeDate": "2026-07-24"}, "note": "Read one session."}],
                  "resultNotes": "Use the returned data date."
                }
                """);

        assertThat(rendered).contains("Read market data.", "Purpose: Read one bounded market snapshot.",
                "Use when:", "Avoid when:", "tradeDate: Optional YYYY-MM-DD date.",
                "{\"tradeDate\":\"2026-07-24\"}", "Result notes: Use the returned data date.");
    }

    @Test
    void ignoresMalformedGuidanceWithoutDroppingBaseDescription() {
        assertThat(AgentToolDescriptionRenderer.render(objectMapper, "Read market data.", "not-json"))
                .isEqualTo("Read market data.");
    }
}
