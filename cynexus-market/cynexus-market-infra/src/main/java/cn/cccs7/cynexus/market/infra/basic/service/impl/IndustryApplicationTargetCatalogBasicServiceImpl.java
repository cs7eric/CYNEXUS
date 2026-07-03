package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.market.infra.basic.entity.IndustryApplicationTargetCatalogPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.IndustryApplicationTargetCatalogMapper;
import cn.cccs7.cynexus.market.infra.basic.service.IndustryApplicationTargetCatalogBasicService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IndustryApplicationTargetCatalogBasicServiceImpl implements IndustryApplicationTargetCatalogBasicService {

    private final IndustryApplicationTargetCatalogMapper mapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public IndustryApplicationTargetCatalogBasicServiceImpl(IndustryApplicationTargetCatalogMapper mapper,
                                                            SnowflakeIdGenerator snowflakeIdGenerator) {
        this.mapper = mapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    @Override
    public List<IndustryApplicationTargetCatalogPO> listAll() {
        return mapper.listAll();
    }

    @Override
    public List<IndustryApplicationTargetCatalogPO> listBySymbols(List<String> symbols) {
        return mapper.listBySymbols(symbols);
    }

    @Override
    public Optional<IndustryApplicationTargetCatalogPO> findBySymbol(String symbol) {
        return Optional.ofNullable(mapper.findBySymbol(symbol));
    }

    @Override
    @Transactional
    public void upsertBatch(List<IndustryApplicationTargetCatalogPO> values) {
        OffsetDateTime now = OffsetDateTime.now();
        for (IndustryApplicationTargetCatalogPO value : values) {
            if (value == null || value.getSymbol() == null || value.getSymbol().isBlank()) {
                continue;
            }
            if (value.getId() == null) {
                value.setId(snowflakeIdGenerator.nextId());
            }
            value.setCreatedAt(value.getCreatedAt() == null ? now : value.getCreatedAt());
            value.setUpdatedAt(now);
            mapper.upsert(value);
        }
    }
}
