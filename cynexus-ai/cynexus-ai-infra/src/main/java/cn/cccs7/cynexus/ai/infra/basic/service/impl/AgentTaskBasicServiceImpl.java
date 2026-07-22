package cn.cccs7.cynexus.ai.infra.basic.service.impl;

import cn.cccs7.cynexus.ai.infra.basic.entity.AgentTaskPO;
import cn.cccs7.cynexus.ai.infra.basic.mapper.AgentTaskMapper;
import cn.cccs7.cynexus.ai.infra.basic.service.AgentTaskBasicService;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AgentTaskBasicServiceImpl implements AgentTaskBasicService {
    private final AgentTaskMapper mapper;
    private final SnowflakeIdGenerator idGenerator;
    public AgentTaskBasicServiceImpl(AgentTaskMapper mapper, SnowflakeIdGenerator idGenerator) { this.mapper = mapper; this.idGenerator = idGenerator; }
    @Override public AgentTaskPO save(AgentTaskPO task) { if (task.getId() == null) task.setId(idGenerator.nextId()); task.setCreatedAt(OffsetDateTime.now()); mapper.insert(task); return task; }
    @Override public List<AgentTaskPO> list(String ownerKey, int limit) { return mapper.selectList(new QueryWrapper<AgentTaskPO>().eq("owner_key", ownerKey).isNull("deleted_at").orderByDesc("created_at").last("LIMIT " + Math.max(1, Math.min(limit, 100)))); }
    @Override public AgentTaskPO find(String ownerKey, String taskNo) { return mapper.selectOne(new QueryWrapper<AgentTaskPO>().eq("owner_key", ownerKey).eq("task_no", taskNo).isNull("deleted_at")); }
    @Override public boolean pause(String ownerKey, String taskNo) { return mapper.pause(ownerKey, taskNo) > 0; }
    @Override public boolean resume(String ownerKey, String taskNo) { return mapper.resume(ownerKey, taskNo) > 0; }
    @Override public boolean cancel(String ownerKey, String taskNo) { return mapper.cancel(ownerKey, taskNo) > 0; }
    @Override public List<AgentTaskPO> claimDue(String workerId, OffsetDateTime now, int limit, int leaseSeconds) { return mapper.claimDue(workerId, now, now.plusSeconds(leaseSeconds), Math.max(1, Math.min(limit, 20))); }
    @Override public void complete(AgentTaskPO task) { mapper.complete(task); }
    @Override public void retry(AgentTaskPO task, OffsetDateTime nextAttemptAt, String error) { task.setNextAttemptAt(nextAttemptAt); task.setLastError(error); mapper.retry(task); }
    @Override public void deadLetter(AgentTaskPO task, String error) { task.setLastError(error); mapper.deadLetter(task); }
}
