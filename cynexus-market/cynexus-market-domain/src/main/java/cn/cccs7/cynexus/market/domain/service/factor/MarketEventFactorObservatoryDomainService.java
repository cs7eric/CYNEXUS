package cn.cccs7.cynexus.market.domain.service.factor;

import cn.cccs7.cynexus.market.domain.entity.factor.MarketEventFactorObservatoryBO;

public interface MarketEventFactorObservatoryDomainService {

    MarketEventFactorObservatoryBO.Result observe(MarketEventFactorObservatoryBO.Query query);
}
