package cn.cccs7.cynexus.ai.infra.basic.service.impl.agent;

import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentMessagePO;
import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentConversationPO;
import cn.cccs7.cynexus.ai.infra.basic.mapper.agent.AgentConversationMapper;
import cn.cccs7.cynexus.ai.infra.basic.mapper.agent.AgentMessageMapper;
import cn.cccs7.cynexus.ai.infra.basic.service.agent.AgentMessageBasicService;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AgentMessageBasicServiceImpl implements AgentMessageBasicService {
    private final AgentMessageMapper mapper;
    private final AgentConversationMapper conversationMapper;
    private final SnowflakeIdGenerator idGenerator;

    public AgentMessageBasicServiceImpl(AgentMessageMapper mapper, AgentConversationMapper conversationMapper,
                                        SnowflakeIdGenerator idGenerator) {
        this.mapper = mapper;
        this.conversationMapper = conversationMapper;
        this.idGenerator = idGenerator;
    }

    @Override
    public void create(AgentMessagePO message) {
        if (message.getId() == null) message.setId(idGenerator.nextId());
        if (message.getOwnerKey() == null) {
            AgentConversationPO conversation = conversationMapper.selectOne(new QueryWrapper<AgentConversationPO>()
                    .eq("conversation_no", message.getConversationNo()).isNull("deleted_at"));
            if (conversation == null) {
                throw new IllegalStateException("Cannot persist Agent message without its conversation: "
                        + message.getConversationNo());
            }
            message.setOwnerKey(conversation.getOwnerKey());
        }
        mapper.insert(message);
    }

    @Override
    public List<AgentMessagePO> find(String ownerKey, String conversationNo) {
        return mapper.selectList(new QueryWrapper<AgentMessagePO>().eq("owner_key", ownerKey)
                .eq("conversation_no", conversationNo)
                .isNull("deleted_at").orderByAsc("sequence_no"));
    }
}
