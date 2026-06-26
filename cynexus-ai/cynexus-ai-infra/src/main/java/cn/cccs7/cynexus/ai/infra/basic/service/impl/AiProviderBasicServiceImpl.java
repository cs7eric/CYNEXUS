package cn.cccs7.cynexus.ai.infra.basic.service.impl;

import cn.cccs7.cynexus.ai.infra.basic.entity.AiProviderPO;
import cn.cccs7.cynexus.ai.infra.basic.mapper.AiProviderMapper;
import cn.cccs7.cynexus.ai.infra.basic.service.AiProviderBasicService;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AiProviderBasicServiceImpl implements AiProviderBasicService {

    private final AiProviderMapper aiProviderMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public AiProviderBasicServiceImpl(AiProviderMapper aiProviderMapper, SnowflakeIdGenerator snowflakeIdGenerator) {
        this.aiProviderMapper = aiProviderMapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    @Override
    public List<AiProviderPO> listAll() {
        return aiProviderMapper.selectList(null);
    }

    @Override
    public AiProviderPO getById(Long id) {
        return aiProviderMapper.selectById(id);
    }

    @Override
    public Long create(AiProviderPO po) {
        if (po.getId() == null) {
            po.setId(snowflakeIdGenerator.nextId());
        }
        aiProviderMapper.insert(po);
        return po.getId();
    }

    @Override
    public boolean update(AiProviderPO po) {
        return aiProviderMapper.updateById(po) > 0;
    }

    @Override
    public boolean delete(Long id) {
        return aiProviderMapper.deleteById(id) > 0;
    }
}
