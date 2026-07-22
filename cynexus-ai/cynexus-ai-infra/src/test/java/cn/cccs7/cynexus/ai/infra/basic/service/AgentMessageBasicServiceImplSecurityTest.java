package cn.cccs7.cynexus.ai.infra.basic.service;

import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentMessagePO;
import cn.cccs7.cynexus.ai.infra.basic.mapper.agent.AgentMessageMapper;
import cn.cccs7.cynexus.ai.infra.basic.mapper.agent.AgentConversationMapper;
import cn.cccs7.cynexus.ai.infra.basic.service.impl.agent.AgentMessageBasicServiceImpl;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AgentMessageBasicServiceImplSecurityTest {

    @Test
    void messageQueryRequiresTheConversationOwner() {
        AgentMessageMapper mapper = mock(AgentMessageMapper.class);
        AgentMessageBasicServiceImpl service = new AgentMessageBasicServiceImpl(mapper, mock(AgentConversationMapper.class),
                mock(SnowflakeIdGenerator.class));

        service.find("tenant-a::user-a", "AGC-1");

        var wrapper = org.mockito.ArgumentCaptor.forClass(QueryWrapper.class);
        verify(mapper).selectList(wrapper.capture());
        assertThat(wrapper.getValue().getCustomSqlSegment()).contains("owner_key", "conversation_no", "deleted_at");
    }
}
