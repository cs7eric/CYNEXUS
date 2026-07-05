package cn.cccs7.cynexus.ai.infra.basic.service.impl;

import cn.cccs7.cynexus.ai.infra.basic.entity.AiPromptPO;
import cn.cccs7.cynexus.ai.infra.basic.mapper.AiPromptMapper;
import cn.cccs7.cynexus.ai.infra.basic.service.AiPromptBasicService;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AiPromptBasicServiceImpl implements AiPromptBasicService {

    private final AiPromptMapper aiPromptMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public AiPromptBasicServiceImpl(AiPromptMapper aiPromptMapper, SnowflakeIdGenerator snowflakeIdGenerator) {
        this.aiPromptMapper = aiPromptMapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    @Override
    public List<AiPromptPO> listAll() {
        return list(null, null, null);
    }

    @Override
    public List<AiPromptPO> list(String keyword, String scenarioCode, String status) {
        LambdaQueryWrapper<AiPromptPO> wrapper = new LambdaQueryWrapper<AiPromptPO>()
                .isNull(AiPromptPO::getDeletedAt)
                .eq(hasText(scenarioCode), AiPromptPO::getScenarioCode, trimToNull(scenarioCode))
                .eq(hasText(status), AiPromptPO::getStatus, trimToNull(status));
        if (hasText(keyword)) {
            String like = trimToNull(keyword);
            wrapper.and(query -> query
                    .like(AiPromptPO::getPromptKey, like)
                    .or()
                    .like(AiPromptPO::getName, like)
                    .or()
                    .like(AiPromptPO::getScenarioCode, like));
        }
        wrapper.orderByDesc(AiPromptPO::getUpdatedAt, AiPromptPO::getCreatedAt);
        return aiPromptMapper.selectList(wrapper);
    }

    @Override
    public AiPromptPO getById(Long id) {
        if (id == null) {
            return null;
        }
        return aiPromptMapper.selectOne(new LambdaQueryWrapper<AiPromptPO>()
                .eq(AiPromptPO::getId, id)
                .isNull(AiPromptPO::getDeletedAt));
    }

    @Override
    public Long create(AiPromptPO po) {
        if (po.getId() == null) {
            po.setId(snowflakeIdGenerator.nextId());
        }
        if (!hasText(po.getPromptVersion())) {
            po.setPromptVersion("v1");
        }
        if (!hasText(po.getStatus())) {
            po.setStatus("draft");
        }
        if (!hasText(po.getVariables())) {
            po.setVariables("[]");
        }
        if (!hasText(po.getTags())) {
            po.setTags("[]");
        }
        if (!hasText(po.getExtra())) {
            po.setExtra("{}");
        }
        aiPromptMapper.insert(po);
        return po.getId();
    }

    @Override
    public boolean update(AiPromptPO po) {
        po.setUpdatedAt(OffsetDateTime.now());
        return aiPromptMapper.updateById(po) > 0;
    }

    @Override
    public boolean delete(Long id) {
        OffsetDateTime now = OffsetDateTime.now();
        return aiPromptMapper.update(null, new LambdaUpdateWrapper<AiPromptPO>()
                .eq(AiPromptPO::getId, id)
                .isNull(AiPromptPO::getDeletedAt)
                .set(AiPromptPO::getUpdatedAt, now)
                .set(AiPromptPO::getDeletedAt, now)) > 0;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }
}
