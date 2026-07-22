package cn.cccs7.cynexus.ai.infra.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentConversationBO;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunBO;
import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentConversationPO;
import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentRunPO;
import cn.cccs7.cynexus.ai.infra.basic.service.agent.AgentConversationBasicService;
import cn.cccs7.cynexus.ai.infra.basic.service.agent.AgentMessageBasicService;
import cn.cccs7.cynexus.ai.infra.basic.service.agent.AgentRunBasicService;
import cn.cccs7.cynexus.ai.infra.basic.service.agent.AgentRunEventBasicService;
import cn.cccs7.cynexus.ai.infra.basic.service.agent.AgentRunRetentionBasicService;
import cn.cccs7.cynexus.ai.infra.repository.agent.AgentRunRepositoryImpl;
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
        AgentRunPO persistedRun = new AgentRunPO();
        persistedRun.setId(202L);
        when(runs.find("owner", "AGR-test")).thenReturn(persistedRun);
        AgentConversationPO persistedConversation = new AgentConversationPO();
        persistedConversation.setId(101L);
        when(conversations.find("owner", "AGC-test")).thenReturn(java.util.Optional.of(persistedConversation));

        AgentRunRepositoryImpl repository = new AgentRunRepositoryImpl(conversations, runs, messages, events, retention);
        AgentConversationBO conversation = AgentConversationBO.builder().conversationNo("AGC-test").ownerKey("owner").build();
        AgentRunBO run = AgentRunBO.builder().runNo("AGR-test").ownerKey("owner").status("RUNNING").build();

        repository.saveConversation(conversation);
        repository.saveRun(run);

        assertThat(conversation.getId()).isEqualTo(101L);
        assertThat(run.getId()).isEqualTo(202L);
        repository.updateConversation(conversation);
        repository.updateRun(run);
        repository.findMessages("owner", "AGC-test");
        verify(conversations).update(any(AgentConversationPO.class));
        verify(runs).update(any(AgentRunPO.class));
        verify(messages).find("owner", "AGC-test");
    }

    @Test
    void resolvesPersistedIdentityWhenAnUpdateReceivesAnUnassignedDomainObject() {
        AgentConversationBasicService conversations = mock(AgentConversationBasicService.class);
        AgentRunBasicService runs = mock(AgentRunBasicService.class);
        AgentMessageBasicService messages = mock(AgentMessageBasicService.class);
        AgentRunEventBasicService events = mock(AgentRunEventBasicService.class);
        AgentRunRetentionBasicService retention = mock(AgentRunRetentionBasicService.class);
        AgentConversationPO persistedConversation = new AgentConversationPO();
        persistedConversation.setId(303L);
        AgentRunPO persistedRun = new AgentRunPO();
        persistedRun.setId(404L);
        when(conversations.find("owner", "AGC-fallback"))
                .thenReturn(java.util.Optional.of(persistedConversation));
        when(runs.find("owner", "AGR-fallback")).thenReturn(persistedRun);

        AgentRunRepositoryImpl repository = new AgentRunRepositoryImpl(conversations, runs, messages, events, retention);
        AgentConversationBO conversation = AgentConversationBO.builder()
                .conversationNo("AGC-fallback").ownerKey("owner").build();
        AgentRunBO run = AgentRunBO.builder().runNo("AGR-fallback").ownerKey("owner").status("FAILED").build();

        repository.updateConversation(conversation);
        repository.updateRun(run);

        assertThat(conversation.getId()).isEqualTo(303L);
        assertThat(run.getId()).isEqualTo(404L);
        verify(conversations).find("owner", "AGC-fallback");
        verify(runs).find("owner", "AGR-fallback");
    }
}
