package cn.cccs7.cynexus.ai.application.agent;

import cn.cccs7.cynexus.ai.domain.policy.sensitivedata.AgentSensitiveDataPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentSensitiveDataPolicyTest {

    @Test
    void redactsCommonCredentialsAndPersonalDataInPlainText() {
        String input = "apiKey=sk-12345678901234567890 bearer abc.def email person@example.com "
                + "mobile 13812345678 aws AKIA1234567890ABCDEF jwt eyJhbGciOiJIUzI1NiJ9.payload.signature";

        String redacted = AgentSensitiveDataPolicy.redactText(input);

        assertThat(redacted).doesNotContain("sk-12345678901234567890", "abc.def", "person@example.com",
                "13812345678", "AKIA1234567890ABCDEF", "eyJhbGciOiJIUzI1NiJ9.payload.signature");
        assertThat(redacted).contains(AgentSensitiveDataPolicy.marker());
    }

    @Test
    void keepsOrdinaryTextWithoutSecretLikePatterns() {
        assertThat(AgentSensitiveDataPolicy.redactText("normal market analysis 2026")).isEqualTo("normal market analysis 2026");
    }
}
