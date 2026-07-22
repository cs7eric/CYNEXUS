package cn.cccs7.cynexus.ai.infra.basic.service;

import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentRunPO;
import cn.cccs7.cynexus.ai.infra.basic.mapper.agent.AgentRunMapper;
import cn.cccs7.cynexus.ai.infra.basic.mapper.agent.AgentRunStepMapper;
import cn.cccs7.cynexus.ai.infra.basic.service.impl.agent.AgentRunBasicServiceImpl;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class AgentRunBasicServiceImplSecurityTest {

    @Test
    void runUpdateIsOwnerScopedEvenWhenThePersistedIdIsKnown() {
        AgentRunMapper runs = mock(AgentRunMapper.class);
        when(runs.update(any(AgentRunPO.class), any(UpdateWrapper.class))).thenReturn(1);
        AgentRunBasicServiceImpl service = new AgentRunBasicServiceImpl(runs, mock(AgentRunStepMapper.class),
                mock(SnowflakeIdGenerator.class));
        AgentRunPO run = new AgentRunPO();
        run.setId(42L);
        run.setRunNo("AGR-1");
        run.setOwnerKey("tenant-a::user-a");
        run.setStatus("RUNNING");

        service.update(run);

        var wrapper = org.mockito.ArgumentCaptor.forClass(UpdateWrapper.class);
        verify(runs).update(any(AgentRunPO.class), wrapper.capture());
        assertThat(wrapper.getValue().getCustomSqlSegment()).contains("owner_key");
    }
}
