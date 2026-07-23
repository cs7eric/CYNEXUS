package cn.cccs7.cynexus.ai.domain.service.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.cccs7.cynexus.ai.domain.entity.AiPromptBO;
import cn.cccs7.cynexus.ai.domain.entity.AiProviderBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisConfigBO;
import cn.cccs7.cynexus.ai.domain.repository.AgentQuotaPolicyRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiProviderRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiUsageBindingRepository;
import cn.cccs7.cynexus.ai.domain.repository.ApplicationAnalysisConfigRepository;
import cn.cccs7.cynexus.ai.domain.repository.ApplicationAnalysisDailySnapshotRepository;
import cn.cccs7.cynexus.ai.domain.repository.ApplicationAnalysisResultCurrentRepository;
import cn.cccs7.cynexus.ai.domain.repository.ApplicationAnalysisResultHistoryRepository;
import cn.cccs7.cynexus.ai.domain.repository.ApplicationAnalysisTargetRepository;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class AiConfigurationDomainServiceTest {

    @Test
    void defaultsPromptVersionAndStatusBeforeCreate() {
        AiPromptBO prompt = AiPromptBO.builder()
                .promptKey("agent").name("Agent").scenarioCode("agent").build();

        service(List.of(), List.of(), List.of()).createPrompt(prompt);

        assertThat(prompt.getPromptVersion()).isEqualTo("v1");
        assertThat(prompt.getStatus()).isEqualTo("draft");
    }

    @Test
    void enabledBindingRequiresEnabledProviderAndActivePrompt() {
        AiProviderBO disabledProvider = AiProviderBO.builder().id(1L).enabled(false).build();
        AiPromptBO draftPrompt = AiPromptBO.builder().id(2L).status("draft").build();
        AiUsageBindingBO binding = AiUsageBindingBO.builder().capability("agent")
                .providerId(1L).promptId(2L).enabled(true).build();

        assertThatThrownBy(() -> service(List.of(disabledProvider), List.of(draftPrompt), List.of()).createUsageBinding(binding))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Enabled AI usage binding requires an enabled provider");

        AiProviderBO enabledProvider = AiProviderBO.builder().id(1L).enabled(true).build();
        assertThatThrownBy(() -> service(List.of(enabledProvider), List.of(draftPrompt), List.of()).createUsageBinding(binding))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Enabled AI usage binding requires an active prompt");
    }

    @Test
    void providerAndPromptCannotBeRemovedOrDeactivatedWhileReferenced() {
        AiProviderBO provider = AiProviderBO.builder().id(1L).code("custom").name("Custom")
                .providerType("custom").enabled(true).build();
        AiPromptBO prompt = AiPromptBO.builder().id(2L).promptKey("agent").name("Agent")
                .scenarioCode("agent").promptVersion("v1").status("active").build();
        AiUsageBindingBO binding = AiUsageBindingBO.builder().id(3L).capability("agent")
                .providerId(1L).promptId(2L).enabled(true).build();
        AiConfigurationDomainService service = service(List.of(provider), List.of(prompt), List.of(binding));

        assertThatThrownBy(() -> service.deleteProvider(1L)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.deletePrompt(2L)).isInstanceOf(IllegalStateException.class);
        AiProviderBO disabledProvider = AiProviderBO.builder().id(1L).code("custom").name("Custom")
                .providerType("custom").enabled(false).build();
        assertThatThrownBy(() -> service.updateProvider(disabledProvider))
                .isInstanceOf(IllegalStateException.class);
        AiPromptBO archivedPrompt = AiPromptBO.builder().id(2L).promptKey("agent").name("Agent")
                .scenarioCode("agent").promptVersion("v1").status("archived").build();
        assertThatThrownBy(() -> service.updatePrompt(archivedPrompt))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsInvalidProviderAndAnalysisConfigState() {
        AiConfigurationDomainService service = service(List.of(), List.of(), List.of());

        assertThatThrownBy(() -> service.createProvider(AiProviderBO.builder().code("custom").name("Custom")
                .providerType("custom").timeoutSeconds(0).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("AI provider timeout must be positive");
        assertThatThrownBy(() -> service.createAnalysisConfig(ApplicationAnalysisConfigBO.builder()
                .configKey("default").status("invalid").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported analysis config status");
    }

    private AiConfigurationDomainService service(List<AiProviderBO> providers,
                                                  List<AiPromptBO> prompts,
                                                  List<AiUsageBindingBO> bindings) {
        return new AiConfigurationDomainServiceImpl(
                repository(AiProviderRepository.class, providers),
                repository(AiPromptRepository.class, prompts),
                repository(AiUsageBindingRepository.class, bindings),
                repository(ApplicationAnalysisConfigRepository.class),
                repository(ApplicationAnalysisDailySnapshotRepository.class),
                repository(ApplicationAnalysisResultCurrentRepository.class),
                repository(ApplicationAnalysisResultHistoryRepository.class),
                repository(ApplicationAnalysisTargetRepository.class),
                repository(AgentQuotaPolicyRepository.class));
    }

    @SuppressWarnings("unchecked")
    private <T> T repository(Class<T> type, List<?> values) {
        InvocationHandler handler = new RepositoryHandler(values);
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private <T> T repository(Class<T> type) { return repository(type, List.of()); }

    private static final class RepositoryHandler implements InvocationHandler {
        private final List<?> values;

        private RepositoryHandler(List<?> values) { this.values = values; }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("listAll") || method.getName().equals("list")) return values;
            if (method.getName().equals("findById")) {
                Long id = args == null || args.length == 0 ? null : (Long) args[0];
                return values.stream().filter(value -> Objects.equals(id, idOf(value)))
                        .findFirst().orElse(null);
            }
            if (method.getName().equals("create")) return 1L;
            if (method.getName().equals("update") || method.getName().equals("delete")) return true;
            if (method.getReturnType().equals(List.class)) return List.of();
            if (method.getReturnType().equals(boolean.class)) return false;
            return null;
        }

        private Long idOf(Object value) {
            try {
                return (Long) value.getClass().getMethod("getId").invoke(value);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Test repository value must expose an id", exception);
            }
        }
    }
}
