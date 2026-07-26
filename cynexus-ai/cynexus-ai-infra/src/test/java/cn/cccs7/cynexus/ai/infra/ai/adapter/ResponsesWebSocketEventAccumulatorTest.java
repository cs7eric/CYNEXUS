package cn.cccs7.cynexus.ai.infra.ai.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelMessage;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelTurnRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResponsesWebSocketEventAccumulatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void accumulatesTextReasoningToolArgumentsUsageAndResponseId() throws Exception {
        ResponsesWebSocketEventAccumulator accumulator = new ResponsesWebSocketEventAccumulator(objectMapper);
        List<String> text = new ArrayList<>();
        List<String> thinking = new ArrayList<>();

        accumulator.accept("{\"type\":\"response.created\",\"response\":{\"id\":\"resp-1\"}}",
                text::add, thinking::add);
        accumulator.accept("{\"type\":\"response.reasoning_summary_text.delta\",\"delta\":\"plan\"}",
                text::add, thinking::add);
        accumulator.accept("{\"type\":\"response.output_text.delta\",\"delta\":\"done\"}",
                text::add, thinking::add);
        accumulator.accept("{\"type\":\"response.output_item.added\",\"item\":{"
                + "\"id\":\"item-1\",\"type\":\"function_call\",\"call_id\":\"call-1\","
                + "\"name\":\"market.stock_research\",\"arguments\":\"\"}}",
                text::add, thinking::add);
        accumulator.accept("{\"type\":\"response.function_call_arguments.delta\","
                + "\"item_id\":\"item-1\",\"delta\":\"{\\\"symbol\\\":\\\"600519\\\"}\"}",
                text::add, thinking::add);
        accumulator.accept("{\"type\":\"response.completed\",\"response\":{\"id\":\"resp-1\","
                + "\"output\":[{\"type\":\"reasoning\",\"encrypted_content\":\"opaque\"},"
                + "{\"type\":\"function_call\",\"call_id\":\"call-1\"}],"
                + "\"usage\":{\"input_tokens\":100,\"input_tokens_details\":{\"cached_tokens\":80},"
                + "\"output_tokens\":10,\"total_tokens\":110}}}", text::add, thinking::add);

        var response = accumulator.toResponse(AgentModelTurnRequest.builder()
                .messages(List.of(AgentModelMessage.builder().content("question").build())).build());

        assertThat(text).containsExactly("done");
        assertThat(thinking).containsExactly("plan");
        assertThat(response.getProviderResponseId()).isEqualTo("resp-1");
        assertThat(response.getProviderOutputJson()).contains("encrypted_content", "function_call");
        assertThat(response.getCachedInputTokens()).isEqualTo(80);
        assertThat(response.getToolCalls()).singleElement().satisfies(call -> {
            assertThat(call.getCallId()).isEqualTo("call-1");
            assertThat(call.getToolCode()).isEqualTo("market.stock_research");
            assertThat(call.getArguments()).containsEntry("symbol", "600519");
        });
    }
}
