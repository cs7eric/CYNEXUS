package cn.cccs7.cynexus.ai.application.agent;

import cn.cccs7.cynexus.ai.domain.entity.capability.AgentCapabilityDescriptor;
import cn.cccs7.cynexus.ai.domain.policy.capability.AgentCapabilityDescriptorPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class AgentCapabilityDescriptorPolicyTest {
    @Test
    void rejectsUnsafeSourceUrl() {
        assertThatIllegalArgumentException().isThrownBy(() -> AgentCapabilityDescriptorPolicy.validate(descriptor("javascript:alert(1)")));
    }

    @Test
    void rejectsUnknownAvailabilityAndOutOfRangePriority() {
        assertThatIllegalArgumentException().isThrownBy(() -> AgentCapabilityDescriptorPolicy.validate(descriptor("/agent" ).toBuilder().availability("BROKEN").build()));
        assertThatIllegalArgumentException().isThrownBy(() -> AgentCapabilityDescriptorPolicy.validateGovernance("AVAILABLE", 10_001));
    }

    @Test
    void dynamicRegistrationRequiresAnActiveReadOnlyAdminDescriptor() {
        assertThatIllegalArgumentException().isThrownBy(() -> AgentCapabilityDescriptorPolicy.validateRegistration(
                descriptor("/agent").toBuilder().registrationMode("HANDLER").build()));
        assertThatIllegalArgumentException().isThrownBy(() -> AgentCapabilityDescriptorPolicy.validateRegistration(
                descriptor("/agent").toBuilder().registrationMode("ADMIN").enabled(false).build()));
    }

    private AgentCapabilityDescriptor descriptor(String sourceUrl) {
        return AgentCapabilityDescriptor.builder().capabilityCode("knowledge.search").version("1.0.0")
                .name("Knowledge Search").module("knowledge").description("Search")
                .inputSchemaJson("{\"type\":\"object\"}").outputSchemaJson("{\"type\":\"array\"}")
                .readOnly(true).requiresConfirmation(false).tenantScope("TENANT").source("test")
                .sourceUrl(sourceUrl).freshness("stored").availability("AVAILABLE")
                .governanceAvailability("AVAILABLE").enabled(true).priority(100).promptKey("autonomous_agent")
                .runtimeEntry("test").routeGroup("Agent / test").build();
    }
}
