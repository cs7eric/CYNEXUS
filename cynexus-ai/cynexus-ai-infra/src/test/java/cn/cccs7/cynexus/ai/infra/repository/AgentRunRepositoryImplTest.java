package cn.cccs7.cynexus.ai.infra.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.cccs7.cynexus.ai.domain.agent.AgentConversationBO;
import cn.cccs7.cynexus.ai.domain.agent.AgentRunBO;
import cn.cccs7.cynexus.ai.infra.basic.entity.AgentConversationPO;
import cn.cccs7.cynexus.ai.infra.basic.entity.AgentRunPO;
import cn.cccs7.cynexus.ai.infra.basic.service.AgentConversationBasicService;
import cn.cccs7.cynexus.ai.infra.basic.service.AgentMessageBasicService;
import cn.cccs7.cynexus.ai.infra.basic.service.AgentRunBasicService;
import cn.cccs7.cynexus.ai.infra.basic.service.AgentRunEventBasicService;
import cn.cccs7.cynexus.ai.infra.basic.service.AgentRunRetentionBasicService;
import org.junit.jupiter.api.Test;

class AgentRunRepositoryImplTest {

    @Test
    void propagatesGeneratedIdsSoLaterRunAndConversationUpdatesPersist() {
        AgentConversationBasicService conversations = mock(AgentConversationBasicService.class);
        AgentRunBasicService runs = mock(AgentRunBasicService.class);
        AgentMessageBasicService messages = mock(AgentMessageBasicService.class);
        AgentRunEventBasicService events = mock(AgentRunEventBasicService.class);
        AgentRunRetentionBasicService retention = mock(AgentRunRetentionBasicService.class);
        doAnswer(invocation -> {
            invocation.getArgument(0, AgentConversationPO.class).setId(101L);
            return null;
        }).when(conversations).create(any(AgentConversationPO.class));
        doAnswer(invocation -> {
            invocation.getArgument(0, AgentRunPO.class).setId(202L);
            return null;
        }).when(runs).create(any(AgentRunPO.class));

        AgentRunRepositoryImpl repository = new AgentRunRepositoryImpl(conversations, runs, messages, events, retention);
        AgentConversationBO conversation = AgentConversationBO.builder().conversationNo("AGC-test").ownerKey("owner").build();
        AgentRunBO run = AgentRunBO.builder().runNo("AGR-test").ownerKey("owner").status("RUNNING").build();

        repository.saveConversation(conversation);
        repository.saveRun(run);

        assertThat(conversation.getId()).isEqualTo(101L);
        assertThat(run.getId()).isEqualTo(202L);
        repository.updateConversation(conversation);
        repository.updateRun(run);
        verify(conversations).update(any(AgentConversationPO.class));
        verify(runs).update(any(AgentRunPO.class));
    }
}
