package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.market.infra.basic.entity.IndustryApplicationConfigPO;
import cn.cccs7.cynexus.market.infra.basic.entity.IndustryApplicationResultPO;
import cn.cccs7.cynexus.market.infra.basic.entity.IndustryApplicationTargetPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.IndustryApplicationMapper;
import cn.cccs7.cynexus.market.infra.basic.service.IndustryApplicationBasicService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IndustryApplicationBasicServiceImpl implements IndustryApplicationBasicService {

    private static final String DEFAULT_CONFIG_KEY = "default";

    private final IndustryApplicationMapper industryApplicationMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public IndustryApplicationBasicServiceImpl(IndustryApplicationMapper industryApplicationMapper,
                                               SnowflakeIdGenerator snowflakeIdGenerator) {
        this.industryApplicationMapper = industryApplicationMapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    @Override
    public IndustryApplicationConfigPO getOrCreateConfig() {
        IndustryApplicationConfigPO current = industryApplicationMapper.findConfig(DEFAULT_CONFIG_KEY);
        if (current != null) {
            return current;
        }
        IndustryApplicationConfigPO created = new IndustryApplicationConfigPO();
        created.setId(snowflakeIdGenerator.nextId());
        created.setConfigKey(DEFAULT_CONFIG_KEY);
        created.setVersion(1);
        created.setHorizonDays(120);
        created.setHorizonSegments(4);
        created.setSource("java-default");
        OffsetDateTime now = OffsetDateTime.now();
        created.setCreatedAt(now);
        created.setUpdatedAt(now);
        industryApplicationMapper.upsertConfig(created);
        return created;
    }

    @Override
    public void saveConfig(IndustryApplicationConfigPO value) {
        if (value.getId() == null) {
            value.setId(snowflakeIdGenerator.nextId());
        }
        value.setConfigKey(DEFAULT_CONFIG_KEY);
        value.setUpdatedAt(OffsetDateTime.now());
        industryApplicationMapper.upsertConfig(value);
    }

    @Override
    public List<IndustryApplicationTargetPO> listTargets() {
        return industryApplicationMapper.listTargets();
    }

    @Override
    public Optional<IndustryApplicationTargetPO> findTarget(String targetKey) {
        return Optional.ofNullable(industryApplicationMapper.findTarget(targetKey));
    }

    @Override
    @Transactional
    public void replaceTargets(List<IndustryApplicationTargetPO> targets) {
        OffsetDateTime now = OffsetDateTime.now();
        industryApplicationMapper.softDeleteAllTargets(now);
        for (IndustryApplicationTargetPO target : targets) {
            if (target.getId() == null) {
                target.setId(snowflakeIdGenerator.nextId());
            }
            target.setCreatedAt(target.getCreatedAt() == null ? now : target.getCreatedAt());
            target.setUpdatedAt(now);
            industryApplicationMapper.upsertTarget(target);
        }
    }

    @Override
    public Optional<IndustryApplicationResultPO> findResult(String targetKey) {
        return Optional.ofNullable(industryApplicationMapper.findResult(targetKey));
    }

    @Override
    public void saveResult(IndustryApplicationResultPO value) {
        OffsetDateTime now = OffsetDateTime.now();
        if (value.getId() == null) {
            value.setId(snowflakeIdGenerator.nextId());
        }
        value.setCreatedAt(value.getCreatedAt() == null ? now : value.getCreatedAt());
        value.setUpdatedAt(now);
        industryApplicationMapper.upsertResult(value);
    }
}
