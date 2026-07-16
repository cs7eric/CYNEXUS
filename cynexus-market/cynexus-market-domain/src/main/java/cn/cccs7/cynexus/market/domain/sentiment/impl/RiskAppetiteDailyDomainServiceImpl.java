package cn.cccs7.cynexus.market.domain.sentiment.impl;

import cn.cccs7.cynexus.market.domain.entity.RiskAppetiteDailyBO;
import cn.cccs7.cynexus.market.domain.repository.RiskAppetiteDailyRepository;
import cn.cccs7.cynexus.market.domain.sentiment.RiskAppetiteDailyDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RiskAppetiteDailyDomainServiceImpl implements RiskAppetiteDailyDomainService {

    private final RiskAppetiteDailyRepository riskAppetiteDailyRepository;

    public RiskAppetiteDailyDomainServiceImpl(RiskAppetiteDailyRepository riskAppetiteDailyRepository) {
        this.riskAppetiteDailyRepository = riskAppetiteDailyRepository;
    }

    @Override
    public List<RiskAppetiteDailyBO> list() {
        return riskAppetiteDailyRepository.list();
    }

    @Override
    public RiskAppetiteDailyBO findById(Long id) {
        return riskAppetiteDailyRepository.findById(id);
    }

    @Override
    public Long create(RiskAppetiteDailyBO value) {
        return riskAppetiteDailyRepository.create(value);
    }

    @Override
    public boolean update(RiskAppetiteDailyBO value) {
        return riskAppetiteDailyRepository.update(value);
    }

    @Override
    public boolean delete(Long id) {
        return riskAppetiteDailyRepository.delete(id);
    }
}
