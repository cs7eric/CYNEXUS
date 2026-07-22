package cn.cccs7.cynexus.ai.domain.policy.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AgentOutboundEndpointPolicyTest {
    @Test
    void acceptsOnlyAnExactConfiguredOrigin() {
        assertThat(AgentOutboundEndpointPolicy.isAllowed(
                "https://api.example.com/v1/chat/completions",
                "https://api.example.com,https://other.example.com")).isTrue();
        assertThat(AgentOutboundEndpointPolicy.isAllowed(
                "https://api.example.com.evil.test/v1/chat/completions",
                "https://api.example.com")).isFalse();
        assertThat(AgentOutboundEndpointPolicy.isAllowed(
                "https://api.example.com:8443/v1/chat/completions",
                "https://api.example.com")).isFalse();
    }

    @Test
    void rejectsCredentialsUnsafeSchemesAndUntrustedDestinations() {
        assertThatThrownBy(() -> AgentOutboundEndpointPolicy.requireAllowed(
                "https://user:secret@api.example.com/v1", "https://api.example.com"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(AgentOutboundEndpointPolicy.isAllowed(
                "file:///etc/passwd", "file:///etc/passwd")).isFalse();
        assertThat(AgentOutboundEndpointPolicy.isAllowed(
                "http://127.0.0.1:33000/api", "https://127.0.0.1:33000")).isFalse();
    }

    @Test
    void permitsExplicitLocalHttpEndpointForDevelopmentOnly() {
        assertThat(AgentOutboundEndpointPolicy.requireAllowed(
                "http://127.0.0.1:33000/api/public/ingestion",
                "http://127.0.0.1:33000").toString())
                .isEqualTo("http://127.0.0.1:33000/api/public/ingestion");
    }
}
