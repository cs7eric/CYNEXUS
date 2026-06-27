package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.RiskAppetiteDailyBO;
import cn.cccs7.cynexus.market.domain.repository.RiskAppetiteDailyRepository;
import cn.cccs7.cynexus.market.infra.basic.service.RiskAppetiteDailyBasicService;
import cn.cccs7.cynexus.market.infra.convert.RiskAppetiteDailyPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class RiskAppetiteDailyRepositoryImpl implements RiskAppetiteDailyRepository {

    private final RiskAppetiteDailyBasicService riskAppetiteDailyBasicService;

    public RiskAppetiteDailyRepositoryImpl(RiskAppetiteDailyBasicService riskAppetiteDailyBasicService) {
        this.riskAppetiteDailyBasicService = riskAppetiteDailyBasicService;
    }

    @Override
    public List<RiskAppetiteDailyBO> list() {
        return RiskAppetiteDailyPOConverter.INSTANCE.toTargetList(riskAppetiteDailyBasicService.listAll());
    }

    @Override
    public RiskAppetiteDailyBO findById(Long id) {
        return Optional.ofNullable(riskAppetiteDailyBasicService.getById(id))
                .map(RiskAppetiteDailyPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(RiskAppetiteDailyBO value) {
        return riskAppetiteDailyBasicService.create(RiskAppetiteDailyPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(RiskAppetiteDailyBO value) {
        return riskAppetiteDailyBasicService.update(RiskAppetiteDailyPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return riskAppetiteDailyBasicService.delete(id);
    }
}
