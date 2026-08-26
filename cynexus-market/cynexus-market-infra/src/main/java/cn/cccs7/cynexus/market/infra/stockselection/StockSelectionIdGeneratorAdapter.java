package cn.cccs7.cynexus.market.infra.stockselection;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.market.domain.stockselection.StockSelectionIdGenerator;
import org.springframework.stereotype.Component;

@Component
public class StockSelectionIdGeneratorAdapter implements StockSelectionIdGenerator {

    private final SnowflakeIdGenerator idGenerator;

    public StockSelectionIdGeneratorAdapter(SnowflakeIdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Override
    public long nextId() {
        return idGenerator.nextId();
    }
}
