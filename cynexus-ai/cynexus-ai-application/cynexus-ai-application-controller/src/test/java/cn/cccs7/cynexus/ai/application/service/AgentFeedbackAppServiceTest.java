package cn.cccs7.cynexus.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.cccs7.cynexus.ai.api.request.AgentFeedbackRequest;
import cn.cccs7.cynexus.ai.domain.agent.AgentFeedbackBO;
import cn.cccs7.cynexus.ai.domain.agent.AgentIdentity;
import cn.cccs7.cynexus.ai.domain.agent.AgentRunBO;
import cn.cccs7.cynexus.ai.domain.repository.AgentFeedbackRepository;
import cn.cccs7.cynexus.ai.domain.repository.AgentRunRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AgentFeedbackAppServiceTest {
    @Test
    void persistsOwnerScopedRedactedFeedbackOnlyAfterTerminalRun() {
        AgentFeedbackRepository feedback = mock(AgentFeedbackRepository.class);
        AgentRunRepository runs = mock(AgentRunRepository.class);
        when(runs.findRun("tenant-1::user-1", "AGR-1")).thenReturn(Optional.of(AgentRunBO.builder()
                .runNo("AGR-1").conversationNo("AGC-1").status("SUCCEEDED").build()));
        when(feedback.upsert(any(AgentFeedbackBO.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AgentFeedbackAppService service = new AgentFeedbackAppService(feedback, runs);
        AgentFeedbackRequest request = new AgentFeedbackRequest();
        request.setRating(1);
        request.setFeedbackReason("Contact me at person@example.com; token Bearer abc123");
        request.setEvaluationLabel("incorrect");

        var result = service.submit(AgentIdentity.builder().tenantKey("tenant-1").principalKey("user-1").build(), "AGR-1", request);

        assertThat(result.getEvaluationLabel()).isEqualTo("INCORRECT");
        AgentFeedbackBO persisted = org.mockito.Mockito.mockingDetails(feedback).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("upsert"))
                .findFirst().orElseThrow().getArgument(0, AgentFeedbackBO.class);
        assertThat(persisted.getOwnerKey()).isEqualTo("tenant-1::user-1");
        assertThat(persisted.getFeedbackReason()).doesNotContain("person@example.com", "abc123");
    }

    @Test
    void rejectsFeedbackForRunningRun() {
        AgentFeedbackRepository feedback = mock(AgentFeedbackRepository.class);
        AgentRunRepository runs = mock(AgentRunRepository.class);
        when(runs.findRun(any(), any())).thenReturn(Optional.of(AgentRunBO.builder().status("RUNNING").build()));
        AgentFeedbackRequest request = new AgentFeedbackRequest();
        request.setRating(5);

        assertThatThrownBy(() -> new AgentFeedbackAppService(feedback, runs).submit(AgentIdentity.singleUser(), "AGR-1", request))
                .isInstanceOf(IllegalStateException.class);
    }
}
