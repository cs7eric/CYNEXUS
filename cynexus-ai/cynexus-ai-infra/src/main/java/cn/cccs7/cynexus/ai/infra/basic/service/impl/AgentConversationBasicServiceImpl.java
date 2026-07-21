package cn.cccs7.cynexus.ai.infra.basic.service.impl;

import cn.cccs7.cynexus.ai.infra.basic.entity.AgentConversationPO;
import cn.cccs7.cynexus.ai.infra.basic.mapper.AgentConversationMapper;
import cn.cccs7.cynexus.ai.infra.basic.service.AgentConversationBasicService;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.Optional;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AgentConversationBasicServiceImpl implements AgentConversationBasicService {
    private final AgentConversationMapper mapper;
    private final SnowflakeIdGenerator idGenerator;

    public AgentConversationBasicServiceImpl(AgentConversationMapper mapper, SnowflakeIdGenerator idGenerator) {
        this.mapper = mapper;
        this.idGenerator = idGenerator;
    }

    @Override
    public void create(AgentConversationPO conversation) {
        if (conversation.getId() == null) conversation.setId(idGenerator.nextId());
        mapper.insert(conversation);
    }

    @Override
    public void update(AgentConversationPO conversation) {
        mapper.updateById(conversation);
    }

    @Override
    public Optional<AgentConversationPO> find(String ownerKey, String conversationNo) {
        return Optional.ofNullable(mapper.selectOne(new QueryWrapper<AgentConversationPO>()
                .eq("owner_key", ownerKey).eq("conversation_no", conversationNo).isNull("deleted_at")));
    }

    @Override
    public List<AgentConversationPO> list(String ownerKey, int limit) {
        return mapper.selectList(new QueryWrapper<AgentConversationPO>().eq("owner_key", ownerKey)
                .isNull("deleted_at").orderByDesc("updated_at").last("LIMIT " + Math.max(1, Math.min(limit, 100))));
    }
}
