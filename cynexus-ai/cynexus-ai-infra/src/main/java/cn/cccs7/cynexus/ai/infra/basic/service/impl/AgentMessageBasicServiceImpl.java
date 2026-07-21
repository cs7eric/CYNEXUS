package cn.cccs7.cynexus.ai.infra.basic.service.impl;

import cn.cccs7.cynexus.ai.infra.basic.entity.AgentMessagePO;
import cn.cccs7.cynexus.ai.infra.basic.mapper.AgentMessageMapper;
import cn.cccs7.cynexus.ai.infra.basic.service.AgentMessageBasicService;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AgentMessageBasicServiceImpl implements AgentMessageBasicService {
    private final AgentMessageMapper mapper;
    private final SnowflakeIdGenerator idGenerator;

    public AgentMessageBasicServiceImpl(AgentMessageMapper mapper, SnowflakeIdGenerator idGenerator) {
        this.mapper = mapper;
        this.idGenerator = idGenerator;
    }

    @Override
    public void create(AgentMessagePO message) {
        if (message.getId() == null) message.setId(idGenerator.nextId());
        mapper.insert(message);
    }

    @Override
    public List<AgentMessagePO> find(String conversationNo) {
        return mapper.selectList(new QueryWrapper<AgentMessagePO>().eq("conversation_no", conversationNo)
                .isNull("deleted_at").orderByAsc("sequence_no"));
    }
}
