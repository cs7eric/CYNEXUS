package cn.cccs7.cynexus.market.infra.portfolio;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioIdGenerator;
import org.springframework.stereotype.Component;

@Component
public class PortfolioIdGeneratorAdapter implements PortfolioIdGenerator {
    private final SnowflakeIdGenerator delegate;

    public PortfolioIdGeneratorAdapter(SnowflakeIdGenerator delegate) {
        this.delegate = delegate;
    }

    @Override
    public long nextId() {
        return delegate.nextId();
    }
}
