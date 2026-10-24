package cn.cccs7.cynexus.ai.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.cccs7.cynexus.ai.api.dto.AiExecuteDTO;
import cn.cccs7.cynexus.ai.api.request.AiExecuteRequest;
import cn.cccs7.cynexus.ai.api.request.AiVisionExecuteRequest;
import cn.cccs7.cynexus.ai.application.service.ScenarioExecutionAppService;
import cn.cccs7.cynexus.ai.application.service.AiVisionTaskAppService;
import cn.cccs7.cynexus.ai.api.dto.AiVisionTaskDTO;
import cn.cccs7.cynexus.ai.api.dto.AiVisionTaskEnqueueDTO;
import cn.cccs7.cynexus.ai.api.dto.AiVisionTaskSummaryDTO;
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
    private AiVisionTaskAppService visionTaskService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(ScenarioExecutionAppService.class);
        visionTaskService = mock(AiVisionTaskAppService.class);
        when(service.execute(any(AiExecuteRequest.class), any(), any()))
                .thenReturn(AiExecuteDTO.builder().executionNo("AI-1").status("SUCCEEDED").build());
        when(service.executeVision(any(AiVisionExecuteRequest.class), any(), any()))
                .thenReturn(AiExecuteDTO.builder().executionNo("AI-2").status("SUCCEEDED").build());
        mockMvc = MockMvcBuilders.standaloneSetup(new AiExecutionController(service,
                new AgentIdentityResolver(SECRET, true), visionTaskService)).build();
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

    @Test
    void visionTaskEnqueueReturnsAcceptedAcknowledgement() throws Exception {
        String principal = "user-a";
        String tenant = "tenant-a";
        when(visionTaskService.enqueue(any(AiVisionExecuteRequest.class), eq(tenant), eq(principal), eq("request-1")))
                .thenReturn(AiVisionTaskEnqueueDTO.builder().accepted(true).processId("42").status("queued")
                        .workflowType("AI_VISION").workflowKey("ai.vision.food_nutrition_label_parse").build());

        mockMvc.perform(post("/api/ai/executions/vision/tasks")
                        .contentType(CommonConstants.MEDIA_TYPE_APPLICATION_JSON)
                        .content("{\"scenarioCode\":\"food_nutrition_label_parse\",\"images\":[{\"dataUrl\":\"data:image/jpeg;base64,abc\"}]}" )
                        .header("Idempotency-Key", "request-1")
                        .header("X-Cynexus-Principal", principal)
                        .header("X-Cynexus-Tenant", tenant)
                        .header("X-Cynexus-Identity-Signature", TrustedPrincipalSigner.sign(principal, tenant, SECRET)))
                .andExpect(status().isAccepted());

        verify(visionTaskService).enqueue(any(AiVisionExecuteRequest.class), eq(tenant), eq(principal), eq("request-1"));
    }

    @Test
    void visionTaskGetReturnsOwnerScopedSnapshot() throws Exception {
        String principal = "user-a";
        String tenant = "tenant-a";
        when(visionTaskService.get(42L, tenant, principal))
                .thenReturn(AiVisionTaskDTO.builder().id("42").status("succeeded").progress(100).build());

        mockMvc.perform(get("/api/ai/executions/vision/tasks/42")
                        .header("X-Cynexus-Principal", principal)
                        .header("X-Cynexus-Tenant", tenant)
                        .header("X-Cynexus-Identity-Signature", TrustedPrincipalSigner.sign(principal, tenant, SECRET)))
                .andExpect(status().isOk());

        verify(visionTaskService).get(42L, tenant, principal);
    }

    @Test
    void visionTaskListRequiresTrustedIdentityAndUsesOwnerScope() throws Exception {
        mockMvc.perform(get("/api/ai/executions/vision/tasks"))
                .andExpect(status().isUnauthorized());

        String principal = "user-a";
        String tenant = "tenant-a";
        when(visionTaskService.list(50, tenant, principal)).thenReturn(java.util.List.of(
                AiVisionTaskSummaryDTO.builder().id("42").status("queued").build()));
        mockMvc.perform(get("/api/ai/executions/vision/tasks")
                        .header("X-Cynexus-Principal", principal)
                        .header("X-Cynexus-Tenant", tenant)
                        .header("X-Cynexus-Identity-Signature", TrustedPrincipalSigner.sign(principal, tenant, SECRET)))
                .andExpect(status().isOk());
        verify(visionTaskService).list(50, tenant, principal);
    }

    @Test
    void visionTaskRetryRequiresTrustedIdentityAndPassesOwnerScope() throws Exception {
        mockMvc.perform(post("/api/ai/executions/vision/tasks/42/retry"))
                .andExpect(status().isUnauthorized());

        String principal = "user-a";
        String tenant = "tenant-a";
        when(visionTaskService.retry(42L, tenant, principal))
                .thenReturn(AiVisionTaskSummaryDTO.builder().id("43").status("queued").build());
        mockMvc.perform(post("/api/ai/executions/vision/tasks/42/retry")
                        .header("X-Cynexus-Principal", principal)
                        .header("X-Cynexus-Tenant", tenant)
                        .header("X-Cynexus-Identity-Signature", TrustedPrincipalSigner.sign(principal, tenant, SECRET)))
                .andExpect(status().isOk());
        verify(visionTaskService).retry(42L, tenant, principal);
    }
}
