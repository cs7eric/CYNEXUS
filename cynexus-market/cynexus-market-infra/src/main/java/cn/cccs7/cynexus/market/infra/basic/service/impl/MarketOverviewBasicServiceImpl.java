package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketOverviewPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.MarketOverviewMapper;
import cn.cccs7.cynexus.market.infra.basic.service.MarketOverviewBasicService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class MarketOverviewBasicServiceImpl extends AbstractCrudBasicService<MarketOverviewPO>
        implements MarketOverviewBasicService {

    private final MarketOverviewMapper marketOverviewMapper;

    public MarketOverviewBasicServiceImpl(MarketOverviewMapper marketOverviewMapper,
                                          SnowflakeIdGenerator snowflakeIdGenerator) {
        super(marketOverviewMapper, snowflakeIdGenerator);
        this.marketOverviewMapper = marketOverviewMapper;
    }

    @Override
    public Optional<MarketOverviewPO> findByTradeDate(String tradeDate) {
        LocalDate date = LocalDate.parse(tradeDate);
        return Optional.ofNullable(marketOverviewMapper.selectOne(new LambdaQueryWrapper<MarketOverviewPO>()
                .eq(MarketOverviewPO::getTradeDate, date)
                .last("limit 1")));
    }
}
