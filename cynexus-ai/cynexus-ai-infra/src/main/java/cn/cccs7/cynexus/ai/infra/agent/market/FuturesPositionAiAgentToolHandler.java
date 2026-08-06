package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.domain.futuresposition.FuturesPositionAiDomainService;
import cn.cccs7.cynexus.ai.domain.handler.toolcall.AgentToolHandler;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class FuturesPositionAiAgentToolHandler implements AgentToolHandler {

    private static final String SOURCE = "Futures Position AI Analysis";
    private static final String INPUT_SCHEMA = """
            {"type":"object","required":["varietyCode","current","history"],"properties":{
              "varietyCode":{"type":"string","enum":["IF","IH","IM","IC"],"description":"Required supported index-futures variety."},
              "instrumentCode":{"type":"string","maxLength":30,"description":"Optional instrument code from the futures query."},
              "instrumentName":{"type":"string","maxLength":80,"description":"Optional instrument display name."},
              "exchangeCode":{"type":"string","maxLength":20,"description":"Optional exchange code from the futures query."},
              "tradeDate":{"type":"string","format":"date","description":"Optional data date in YYYY-MM-DD format."},
              "top":{"type":"integer","minimum":1,"maximum":50,"description":"Optional bounded member ranking size."},
              "current":{"type":"object","required":["instrumentCode"],"description":"Required current position snapshot from the futures query.","properties":{
                "exchangeCode":{"type":"string","maxLength":20,"description":"Exchange code for the current snapshot."},
                "instrumentCode":{"type":"string","minLength":1,"maxLength":40,"description":"Instrument code for the current snapshot."},
                "varietyCode":{"type":"string","maxLength":20,"description":"Variety code in the current snapshot."},
                "contractCode":{"type":"string","maxLength":40,"description":"Contract code in the current snapshot."},
                "volume":{"type":"number","description":"Current traded volume."},
                "longPosition":{"type":"number","description":"Current long position."},
                "shortPosition":{"type":"number","description":"Current short position."},
                "netPosition":{"type":"number","description":"Current net position."},
                "longShortRatio":{"type":"number","description":"Current long-short ratio."},
                "longChange":{"type":"number","description":"Current long-position change."},
                "shortChange":{"type":"number","description":"Current short-position change."},
                "source":{"type":"string","maxLength":80,"description":"Source label for the current snapshot."}
              },"additionalProperties":false},
              "history":{"type":"array","maxItems":1100,"description":"Required bounded historical position rows.","items":{"type":"object","properties":{
                "tradeDate":{"type":"string","format":"date","description":"Historical observation date."},
                "instrumentCode":{"type":"string","maxLength":40,"description":"Historical instrument code."},
                "varietyCode":{"type":"string","maxLength":20,"description":"Historical variety code."},
                "volume":{"type":"number","description":"Historical volume."},
                "longPosition":{"type":"number","description":"Historical long position."},
                "shortPosition":{"type":"number","description":"Historical short position."},
                "netPosition":{"type":"number","description":"Historical net position."},
                "longShortRatio":{"type":"number","description":"Historical long-short ratio."},
                "longChange":{"type":"number","description":"Historical long-position change."},
                "shortChange":{"type":"number","description":"Historical short-position change."},
                "source":{"type":"string","maxLength":80,"description":"Source label for the historical row."}
              },"additionalProperties":false}},
              "members":{"type":"array","maxItems":90,"description":"Bounded member ranking rows.","items":{"type":"object","properties":{
                "tradeDate":{"type":"string","format":"date","description":"Member ranking date."},
                "memberName":{"type":"string","maxLength":120,"description":"Member name."},
                "rankType":{"type":"string","maxLength":20,"description":"Ranking dimension."},
                "rankNo":{"type":"integer","minimum":1,"maximum":100,"description":"Member rank number."},
                "rankValue":{"type":"number","description":"Member ranking value."},
                "rankChange":{"type":"number","description":"Member ranking change."},
                "source":{"type":"string","maxLength":80,"description":"Source label for the member row."}
              },"additionalProperties":false}}
            },"additionalProperties":false}
            """;
    private static final String OUTPUT_SCHEMA = """
            {"type":"object","required":["analysis_result"],"properties":{"analysis_result":{"type":"object","required":["headline","structure","evidence","trend","risk","watch","data_quality","disclaimer"],"properties":{
              "headline":{"type":"string","description":"Factual analysis headline."},
              "structure":{"type":"string","description":"Current position-structure summary."},
              "evidence":{"type":"array","items":{"type":"string"},"description":"Evidence statements grounded in input data."},
              "trend":{"type":"string","description":"Observed historical trend summary."},
              "risk":{"type":"string","description":"Data or interpretation risk note."},
              "watch":{"type":"array","items":{"type":"string"},"description":"Conditions to continue monitoring."},
              "data_quality":{"type":"string","description":"Input coverage and quality note."},
              "disclaimer":{"type":"string","description":"Facts-only analysis disclaimer."}
            },"additionalProperties":false}},"additionalProperties":false}
            """;
    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder()
            .code(FuturesPositionAiDomainService.CAPABILITY)
            .name("Futures position structure analysis")
            .module("market")
            .description("Analyze bounded IF, IH, IM, or IC member-position structure without producing a trading signal.")
            .guidanceJson(FuturesPositionAiAgentToolGuidance.ANALYSIS)
            .inputSchemaJson(INPUT_SCHEMA).outputSchemaJson(OUTPUT_SCHEMA)
            .readOnly(true).requiresConfirmation(false)
            .freshness("provided futures position snapshot")
            .source(SOURCE).sourceUrl("/market/futures-position")
            .promptKey(FuturesPositionAiDomainService.PROMPT_KEY)
            .routeGroup("Agent / market")
            .build();

    private final FuturesPositionAiDomainService domainService;
    private final ObjectMapper objectMapper;

    public FuturesPositionAiAgentToolHandler(FuturesPositionAiDomainService domainService, ObjectMapper objectMapper) {
        this.domainService = domainService;
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        Map<String, Object> arguments = call.getArguments() == null ? Map.of() : call.getArguments();
        try {
            FuturesPositionAiDomainService.AnalysisResult result = domainService.analyze(
                    new FuturesPositionAiDomainService.AnalysisCommand(
                            string(arguments.get("varietyCode")), string(arguments.get("instrumentCode")),
                            string(arguments.get("instrumentName")), string(arguments.get("exchangeCode")),
                            string(arguments.get("tradeDate")), integer(arguments.get("top")),
                            object(arguments.get("current")), list(arguments.get("history")), list(arguments.get("members"))));
            ObjectNode output = objectMapper.createObjectNode();
            output.set("analysis_result", result.analysis());
            return AgentToolResult.success(DEFINITION.getCode(), output.toString(),
                    "Generated a factual futures position structure analysis.", result.tradeDate(), SOURCE, List.of());
        } catch (IllegalArgumentException exception) {
            return AgentToolResult.failure(DEFINITION.getCode(), "Futures position analysis arguments are invalid.", SOURCE, "ARGUMENT_INVALID");
        } catch (Exception exception) {
            return AgentToolResult.failure(DEFINITION.getCode(), "Futures position analysis failed.", SOURCE, "FUTURES_POSITION_ANALYSIS_FAILED");
        }
    }

    private Map<String, Object> object(Object value) {
        return value == null ? Map.of() : objectMapper.convertValue(value, new TypeReference<>() { });
    }

    private List<Map<String, Object>> list(Object value) {
        return value == null ? List.of() : objectMapper.convertValue(value, new TypeReference<>() { });
    }

    private Integer integer(Object value) {
        try { return value == null ? null : Integer.valueOf(String.valueOf(value)); }
        catch (NumberFormatException ignored) { return null; }
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
