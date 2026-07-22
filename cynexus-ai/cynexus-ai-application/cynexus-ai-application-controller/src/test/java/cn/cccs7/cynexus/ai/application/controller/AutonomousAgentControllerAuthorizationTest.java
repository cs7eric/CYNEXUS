package cn.cccs7.cynexus.ai.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.cccs7.cynexus.ai.api.dto.AgentRunDTO;
import cn.cccs7.cynexus.ai.application.controller.agent.AutonomousAgentController;
import cn.cccs7.cynexus.ai.application.service.agent.identity.AgentIdentityResolver;
import cn.cccs7.cynexus.ai.application.service.agent.execution.AutonomousAgentAppService;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentIdentity;
import cn.cccs7.cynexus.common.security.identity.TrustedPrincipalSigner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AutonomousAgentControllerAuthorizationTest {
    private static final String SECRET = "controller-test-secret-012345678901234567890";
    private AutonomousAgentAppService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(AutonomousAgentAppService.class);
        when(service.getRun(any(AgentIdentity.class), anyString())).thenReturn(AgentRunDTO.builder().runNo("AGR-1").build());
        mockMvc = MockMvcBuilders.standaloneSetup(new AutonomousAgentController(service,
                new AgentIdentityResolver(SECRET, false))).build();
    }

    @Test
    void signedIdentityControlsOwnerAndClientOwnerParameterIsIgnored() throws Exception {
        String principal = "user-a";
        String tenant = "tenant-a";
        mockMvc.perform(get("/api/ai/agent/runs/AGR-1?ownerKey=tenant-b::user-b")
                        .header("X-Cynexus-Principal", principal)
                        .header("X-Cynexus-Tenant", tenant)
                        .header("X-Cynexus-Identity-Signature", TrustedPrincipalSigner.sign(principal, tenant, SECRET)))
                .andExpect(status().isOk());

        ArgumentCaptor<AgentIdentity> identity = ArgumentCaptor.forClass(AgentIdentity.class);
        verify(service).getRun(identity.capture(), org.mockito.ArgumentMatchers.eq("AGR-1"));
        assertEquals("tenant-a::user-a", identity.getValue().storageOwnerKey());
    }

    @Test
    void missingSignatureIsRejectedBeforeApplicationService() throws Exception {
        mockMvc.perform(get("/api/ai/agent/runs/AGR-1")
                        .header("X-Cynexus-Principal", "user-a")
                        .header("X-Cynexus-Tenant", "tenant-a"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void signatureCannotBeReplayedForAnotherTenant() throws Exception {
        String principal = "user-a";
        String signedTenant = "tenant-a";
        mockMvc.perform(get("/api/ai/agent/runs/AGR-1")
                        .header("X-Cynexus-Principal", principal)
                        .header("X-Cynexus-Tenant", "tenant-b")
                        .header("X-Cynexus-Identity-Signature", TrustedPrincipalSigner.sign(principal, signedTenant, SECRET)))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void capabilityGovernanceRequiresAdministratorPrincipal() throws Exception {
        String principal = "user-a";
        String tenant = "tenant-a";
        mockMvc.perform(post("/api/ai/agent/capabilities/market.overview/governance")
                        .contentType("application/json").content("{\"enabled\":false,\"availability\":\"AVAILABLE\",\"priority\":1}")
                        .header("X-Cynexus-Principal", principal).header("X-Cynexus-Tenant", tenant)
                        .header("X-Cynexus-Identity-Signature", TrustedPrincipalSigner.sign(principal, tenant, SECRET)))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
}
