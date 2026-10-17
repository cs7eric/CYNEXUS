package cn.cccs7.cynexus.ai.infra.ai.prompt;

import static org.junit.jupiter.api.Assertions.*;
import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.entity.AiPromptBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiPromptRuntimeResolverImplTest {
    @Test void bindingPromptWinsAndInactiveBindingFallsBackToRequestedVersion() {
        AiPromptBO bound = AiPromptBO.builder().id(7L).promptKey("cap").promptVersion("bound")
                .status("active").systemPrompt("bound-system").userPromptTemplate("{{value}}").build();
        AiPromptBO versioned = AiPromptBO.builder().id(8L).promptKey("cap").promptVersion("v2")
                .status("active").systemPrompt("version-system").userPromptTemplate("{{value}}").build();
        var routing = new Routing(AiUsageBindingBO.builder().capability("cap").promptId(7L).params("{\"prompt_version\":\"v2\"}").build(), "v2");
        var repo = new Repo(List.of(versioned), bound);
        var resolver = new AiPromptRuntimeResolverImpl(routing, repo);
        assertEquals("bound", resolver.resolve("cap", "cap", "fallback", "{{value}}").promptVersion());

        routing.binding = AiUsageBindingBO.builder().capability("cap").promptId(99L).params("{\"prompt_version\":\"v2\"}").build();
        assertEquals("v2", resolver.resolve("cap", "cap", "fallback", "{{value}}").promptVersion());
        assertEquals("version-system" + System.lineSeparator() + "value", resolver.resolve("cap", "cap", "fallback", "{{value}}").render(java.util.Map.of("value", "value")));
    }

    @Test void mismatchedBindingKeyIsIgnoredAndMissingVersionUsesActivePrompt() {
        AiPromptBO wrongKey = AiPromptBO.builder().id(7L).promptKey("other-capability").promptVersion("v9")
                .status("active").systemPrompt("wrong").userPromptTemplate("wrong").build();
        AiPromptBO active = AiPromptBO.builder().id(8L).promptKey("cap").promptVersion("v3")
                .status("active").systemPrompt("active").userPromptTemplate("{{value}}").build();
        var routing = new Routing(AiUsageBindingBO.builder().capability("cap").promptId(7L)
                .params("{\"prompt_version\":\"missing\"}").build(), "missing");
        var resolver = new AiPromptRuntimeResolverImpl(routing, new Repo(List.of(active), wrongKey));

        var resolved = resolver.resolve("cap", "cap", "fallback", "{{value}}");
        assertEquals("cap", resolved.promptKey());
        assertEquals("v3", resolved.promptVersion());
        assertEquals("active", resolved.systemPrompt());
    }

    private static final class Routing implements ApplicationAnalysisRoutingDomainService {
        private AiUsageBindingBO binding; private final String version;
        Routing(AiUsageBindingBO binding, String version) { this.binding = binding; this.version = version; }
        public AiUsageBindingBO findBinding(String capability) { return binding; }
        public List<cn.cccs7.cynexus.ai.domain.entity.AiModelBO> routeModels(String c, String p) { return List.of(); }
        public String promptVersion(AiUsageBindingBO b) { return version; }
    }
    private record Repo(List<AiPromptBO> prompts, AiPromptBO bound) implements AiPromptRepository {
        public List<AiPromptBO> list(String k, String s, String status) { return prompts; }
        public AiPromptBO findById(Long id) { return id != null && id == 7L ? bound : null; }
        public Long create(AiPromptBO p) { return 1L; }
        public boolean update(AiPromptBO p) { return true; }
        public boolean delete(Long id) { return true; }
    }
}
