package cn.cccs7.cynexus.market.infra.factorresearch;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorStudyIdGenerator;
import org.springframework.stereotype.Component;

@Component
public class MarketFactorStudyIdGeneratorAdapter implements MarketFactorStudyIdGenerator {
    private final SnowflakeIdGenerator idGenerator;

    public MarketFactorStudyIdGeneratorAdapter(SnowflakeIdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Override
    public String nextStudyId() {
        return "mfs_" + idGenerator.nextId();
    }
}
