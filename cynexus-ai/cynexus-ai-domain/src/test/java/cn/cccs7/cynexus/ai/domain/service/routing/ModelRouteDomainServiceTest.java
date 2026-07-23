package cn.cccs7.cynexus.ai.domain.service.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiScenarioBO;
import cn.cccs7.cynexus.ai.domain.repository.AiModelRepository;
import cn.cccs7.cynexus.ai.domain.routing.ModelRoutingDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.valueobject.ModelCapability;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModelRouteDomainServiceTest {

    @Test
    void loadsCandidatesFromRepositoryBeforeApplyingRoutingRules() {
        AiModelBO model = model("preferred", true);
        ModelRouteDomainService service = new ModelRouteDomainServiceImpl(
                () -> List.of(model), new ModelRoutingDomainServiceImpl());

        AiModelBO result = service.route(scenario(), "preferred");

        assertThat(result).isSameAs(model);
    }

    @Test
    void propagatesRoutingFailureWhenRepositoryHasNoCompatibleModel() {
        ModelRouteDomainService service = new ModelRouteDomainServiceImpl(
                () -> List.of(model("other", true)), new ModelRoutingDomainServiceImpl());

        assertThatThrownBy(() -> service.route(scenario(), "preferred"))
                .hasMessage("No available model for scenario");
    }

    private AiScenarioBO scenario() {
        return AiScenarioBO.builder()
                .scenarioCode("test")
                .toolCallRequired(false)
                .jsonOutputRequired(true)
                .enabled(true)
                .build();
    }

    private AiModelBO model(String code, boolean enabled) {
        return AiModelBO.builder()
                .modelCode(code)
                .enabled(enabled)
                .capability(ModelCapability.builder().structuredOutputSupported(true).build())
                .build();
    }
}
