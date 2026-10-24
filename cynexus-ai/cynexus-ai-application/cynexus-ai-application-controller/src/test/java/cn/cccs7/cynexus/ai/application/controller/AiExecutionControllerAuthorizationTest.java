package cn.cccs7.cynexus.ai.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.cccs7.cynexus.ai.api.dto.AiExecuteDTO;
import cn.cccs7.cynexus.ai.api.request.AiExecuteRequest;
import cn.cccs7.cynexus.ai.api.request.AiVisionExecuteRequest;
import cn.cccs7.cynexus.ai.application.service.ScenarioExecutionAppService;
import cn.cccs7.cynexus.ai.application.service.agent.identity.AgentIdentityResolver;
import cn.cccs7.cynexus.common.core.constant.CommonConstants;
import cn.cccs7.cynexus.common.security.identity.TrustedPrincipalSigner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AiExecutionControllerAuthorizationTest {
    private static final String SECRET = "controller-test-secret-012345678901234567890";
    private ScenarioExecutionAppService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(ScenarioExecutionAppService.class);
        when(service.execute(any(AiExecuteRequest.class), any(), any()))
                .thenReturn(AiExecuteDTO.builder().executionNo("AI-1").status("SUCCEEDED").build());
        when(service.executeVision(any(AiVisionExecuteRequest.class), any(), any()))
                .thenReturn(AiExecuteDTO.builder().executionNo("AI-2").status("SUCCEEDED").build());
        mockMvc = MockMvcBuilders.standaloneSetup(new AiExecutionController(service,
                new AgentIdentityResolver(SECRET, true))).build();
    }

    @Test
    void fridgePreviewRejectsAnonymousRequestEvenWhenGenericAiAllowsAnonymous() throws Exception {
        mockMvc.perform(post("/api/ai/executions")
                        .contentType(CommonConstants.MEDIA_TYPE_APPLICATION_JSON)
                        .content("{\"scenarioCode\":\"food_fridge_inventory\",\"userInput\":\"milk\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void fridgePreviewPassesVerifiedTenantAndPrincipalToExecution() throws Exception {
        String principal = "user-a";
        String tenant = "tenant-a";
        mockMvc.perform(post("/api/ai/executions")
                        .contentType(CommonConstants.MEDIA_TYPE_APPLICATION_JSON)
                        .content("{\"scenarioCode\":\"food_fridge_inventory\",\"userInput\":\"milk\"}")
                        .header("X-Cynexus-Principal", principal)
                        .header("X-Cynexus-Tenant", tenant)
                        .header("X-Cynexus-Identity-Signature", TrustedPrincipalSigner.sign(principal, tenant, SECRET)))
                .andExpect(status().isOk());

        verify(service).execute(any(AiExecuteRequest.class), eq(tenant), eq(principal));
    }

    @Test
    void otherGenericScenariosKeepTheirAnonymousCompatibility() throws Exception {
        mockMvc.perform(post("/api/ai/executions")
                        .contentType(CommonConstants.MEDIA_TYPE_APPLICATION_JSON)
                        .content("{\"scenarioCode\":\"text_summary\",\"userInput\":\"text\"}"))
                .andExpect(status().isOk());

        verify(service).execute(any(AiExecuteRequest.class), eq(null), eq(null));
    }

    @Test
    void foodNutritionVisionRejectsAnonymousRequest() throws Exception {
        mockMvc.perform(post("/api/ai/executions/vision")
                        .contentType(CommonConstants.MEDIA_TYPE_APPLICATION_JSON)
                        .content("{\"scenarioCode\":\"food_nutrition_label_parse\",\"images\":[]}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void foodNutritionVisionPassesVerifiedTenantAndPrincipalToExecution() throws Exception {
        String principal = "user-a";
        String tenant = "tenant-a";
        mockMvc.perform(post("/api/ai/executions/vision")
                        .contentType(CommonConstants.MEDIA_TYPE_APPLICATION_JSON)
                        .content("{\"scenarioCode\":\"food_nutrition_label_parse\",\"images\":[]}")
                        .header("X-Cynexus-Principal", principal)
                        .header("X-Cynexus-Tenant", tenant)
                        .header("X-Cynexus-Identity-Signature", TrustedPrincipalSigner.sign(principal, tenant, SECRET)))
                .andExpect(status().isOk());

        verify(service).executeVision(any(AiVisionExecuteRequest.class), eq(tenant), eq(principal));
    }
}
