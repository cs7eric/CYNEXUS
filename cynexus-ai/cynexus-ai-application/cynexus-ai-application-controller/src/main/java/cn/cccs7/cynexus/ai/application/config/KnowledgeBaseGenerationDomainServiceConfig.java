package cn.cccs7.cynexus.ai.application.config;

import cn.cccs7.cynexus.ai.api.dto.KnowledgeBaseGenerateDTO;
import cn.cccs7.cynexus.ai.application.prompt.KnowledgeBaseStructuredOutputParser;
import cn.cccs7.cynexus.ai.application.prompt.StructuredJsonExtractor;
import cn.cccs7.cynexus.ai.application.service.ModelRouteAppService;
import cn.cccs7.cynexus.ai.application.service.agent.execution.AgentExecutor;
import cn.cccs7.cynexus.ai.domain.entity.AiScenarioBO;
import cn.cccs7.cynexus.ai.domain.knowledgebase.KnowledgeBaseDocumentDraftPolicy;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.KnowledgeBaseDocumentGenerationDomainService;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.KnowledgeBaseDocumentGenerationPromptPolicy;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.KnowledgeBaseGenerationParsedDocument;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.KnowledgeBaseGenerationRoutingParameters;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.impl.KnowledgeBaseDocumentGenerationDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.port.KnowledgeBaseGenerationCompletionPort;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.port.KnowledgeBaseGenerationFallbackModelPort;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.port.KnowledgeBaseGenerationOutputParserPort;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.port.KnowledgeBaseGenerationRoutingParametersPort;
import cn.cccs7.cynexus.ai.domain.repository.AiExecutionLogRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiProviderRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiUsageBindingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KnowledgeBaseGenerationDomainServiceConfig {

    @Bean
    public KnowledgeBaseDocumentGenerationDomainService knowledgeBaseDocumentGenerationDomainService(
            AiUsageBindingRepository usageBindingRepository,
            AiProviderRepository providerRepository,
            AiPromptRepository promptRepository,
            AiExecutionLogRepository executionLogRepository,
            ModelRouteAppService modelRouteAppService,
            AgentExecutor agentExecutor,
            StructuredJsonExtractor structuredJsonExtractor,
            KnowledgeBaseStructuredOutputParser outputParser) {
        return new KnowledgeBaseDocumentGenerationDomainServiceImpl(
                usageBindingRepository, providerRepository, promptRepository, executionLogRepository,
                preferredModelCode -> modelRouteAppService.route(buildScenario(), preferredModelCode),
                routingParametersPort(),
                (model, prompt, capability) -> agentExecutor.completeDetailed(model, prompt, capability),
                outputParserPort(structuredJsonExtractor, outputParser),
                new KnowledgeBaseDocumentDraftPolicy(), new KnowledgeBaseDocumentGenerationPromptPolicy());
    }

    private KnowledgeBaseGenerationRoutingParametersPort routingParametersPort() {
        ObjectMapper mapper = new ObjectMapper();
        return rawParams -> {
            JsonNode params = readJson(mapper, rawParams);
            return new KnowledgeBaseGenerationRoutingParameters(normalizedValues(params.get("tenant_code")),
                    normalizedValues(params.get("quality")), normalizedValues(params.get("cost_tier")),
                    normalizedScalar(params.get("prompt_version")), orderedValues(params, "fallback_chain", "fallback_models"));
        };
    }

    private KnowledgeBaseGenerationOutputParserPort outputParserPort(StructuredJsonExtractor extractor,
                                                                       KnowledgeBaseStructuredOutputParser parser) {
        return new KnowledgeBaseGenerationOutputParserPort() {
            @Override public String formatInstructions() { return parser.getFormat(); }

            @Override public KnowledgeBaseGenerationParsedDocument parse(String rawOutput) throws Exception {
                KnowledgeBaseGenerateDTO value = parser.convert(extractor.extractObject(rawOutput));
                return new KnowledgeBaseGenerationParsedDocument(value.getTitle(), value.getDescription(),
                        value.getTags(), value.getCategories(), value.getContent());
            }
        };
    }

    private AiScenarioBO buildScenario() {
        return AiScenarioBO.builder().scenarioCode("knowledge_base_document_generate")
                .scenarioName("Knowledge Base Document Generation").maxToolCalls(0).toolCallRequired(false)
                .jsonOutputRequired(true).enabled(true).build();
    }

    private JsonNode readJson(ObjectMapper mapper, String rawJson) {
        if (rawJson == null || rawJson.isBlank()) return mapper.createObjectNode();
        try {
            JsonNode value = mapper.readTree(rawJson);
            return value == null || !value.isObject() ? mapper.createObjectNode() : value;
        } catch (Exception ignored) { return mapper.createObjectNode(); }
    }

    private Set<String> normalizedValues(JsonNode node) {
        Set<String> values = new LinkedHashSet<>();
        if (node == null || node.isNull()) return values;
        if (node.isArray()) node.forEach(item -> add(values, item.asText("")));
        else add(values, node.asText(""));
        return values;
    }

    private List<String> orderedValues(JsonNode params, String... fields) {
        Set<String> values = new LinkedHashSet<>();
        for (String field : fields) {
            JsonNode node = params == null ? null : params.get(field);
            if (node != null && node.isArray()) node.forEach(item -> add(values, item.asText("")));
        }
        return new ArrayList<>(values);
    }

    private void add(Set<String> values, String value) {
        String normalized = value == null ? "" : value.trim();
        if (!normalized.isBlank()) values.add(normalized.toLowerCase());
    }

    private String normalizedScalar(JsonNode node) { return node == null || node.isNull() ? "" : node.asText("").trim(); }
}
