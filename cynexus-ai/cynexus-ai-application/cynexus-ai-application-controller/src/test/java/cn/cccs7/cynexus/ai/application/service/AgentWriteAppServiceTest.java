package cn.cccs7.cynexus.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.cccs7.cynexus.ai.api.request.AgentWriteConfirmRequest;
import cn.cccs7.cynexus.ai.api.request.AgentWritePreviewRequest;
import cn.cccs7.cynexus.ai.application.service.agent.write.AgentWriteAppService;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentIdentity;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentWriteCommandBO;
import cn.cccs7.cynexus.ai.domain.gateway.capability.AgentWriteExecutorPort;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentWriteResult;
import cn.cccs7.cynexus.ai.domain.repository.agent.AgentWriteCommandRepository;
import cn.cccs7.cynexus.ai.domain.service.agent.write.AgentWriteDomainService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AgentWriteAppServiceTest {
    @Test
    void previewIssuesExpiringTokenAndConfirmExecutesOnlyTheBoundCommand() {
        AgentWriteCommandRepository repository = mock(AgentWriteCommandRepository.class);
        AgentWriteExecutorPort executor = mock(AgentWriteExecutorPort.class);
        when(repository.save(any(AgentWriteCommandBO.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AgentWriteAppService service = new AgentWriteAppService(new AgentWriteDomainService(repository, executor, new ObjectMapper()));
        AgentWritePreviewRequest request = new AgentWritePreviewRequest();
        request.setToolCode("knowledge.document.create");
        request.setArgumentsJson("{\"title\":\"Draft\",\"content\":\"Content\",\"status\":\"DRAFT\"}");

        var preview = service.preview(AgentIdentity.singleUser(), request);
        assertThat(preview.getConfirmationToken()).isNotBlank();
        assertThat(preview.getExpiresAt()).isAfter(java.time.OffsetDateTime.now());
        AgentWriteCommandBO confirmed = AgentWriteCommandBO.builder().commandNo(preview.getCommandNo()).ownerKey("default::single-user")
                .status("CONFIRMED").toolCode("knowledge.document.create").argumentsJson(request.getArgumentsJson())
                .expiresAt(java.time.OffsetDateTime.now().plusMinutes(5)).build();
        when(repository.findPending("default::single-user", preview.getCommandNo())).thenReturn(Optional.of(confirmed));
        when(repository.markConfirmed(any(), any(), any())).thenReturn(Optional.of(confirmed));
        when(executor.execute(any())).thenReturn(AgentWriteResult.builder().success(true).resultJson("{\"documentId\":1}").build());
        AgentWriteConfirmRequest confirm = new AgentWriteConfirmRequest();
        confirm.setConfirmationToken(preview.getConfirmationToken());

        var result = service.confirm(AgentIdentity.singleUser(), preview.getCommandNo(), confirm);

        assertThat(result.getStatus()).isEqualTo("EXECUTED");
        verify(executor).execute(any(AgentWriteCommandBO.class));
        verify(repository).markExecuted(any(AgentWriteCommandBO.class));
    }
}
