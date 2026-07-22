package cn.cccs7.cynexus.ai.application.agent;

import cn.cccs7.cynexus.ai.domain.policy.sensitivedata.AgentSourceUrlPolicy;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentSourceUrlPolicyTest {
    @Test
    void permitsRelativeAndTrustedHttpsSourcesOnly() {
        assertThat(AgentSourceUrlPolicy.sanitize("/stock-overview", Set.of())).isEqualTo("/stock-overview");
        assertThat(AgentSourceUrlPolicy.sanitize("https://trusted.example/source", Set.of("https://trusted.example")))
                .isEqualTo("https://trusted.example/source");
        assertThat(AgentSourceUrlPolicy.sanitize("//evil.example/source", Set.of())).isNull();
        assertThat(AgentSourceUrlPolicy.sanitize("javascript:alert(1)", Set.of())).isNull();
        assertThat(AgentSourceUrlPolicy.sanitize("https://evil.example/source", Set.of("https://trusted.example"))).isNull();
    }
}
