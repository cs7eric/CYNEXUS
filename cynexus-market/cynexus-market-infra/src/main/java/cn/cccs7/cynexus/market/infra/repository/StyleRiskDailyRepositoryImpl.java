package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.StyleRiskDailyBO;
import cn.cccs7.cynexus.market.domain.repository.StyleRiskDailyRepository;
import cn.cccs7.cynexus.market.infra.basic.service.StyleRiskDailyBasicService;
import cn.cccs7.cynexus.market.infra.convert.StyleRiskDailyPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class StyleRiskDailyRepositoryImpl implements StyleRiskDailyRepository {

    private final StyleRiskDailyBasicService styleRiskDailyBasicService;

    public StyleRiskDailyRepositoryImpl(StyleRiskDailyBasicService styleRiskDailyBasicService) {
        this.styleRiskDailyBasicService = styleRiskDailyBasicService;
    }

    @Override
    public List<StyleRiskDailyBO> list() {
        return StyleRiskDailyPOConverter.INSTANCE.toTargetList(styleRiskDailyBasicService.listAll());
    }

    @Override
    public StyleRiskDailyBO findById(Long id) {
        return Optional.ofNullable(styleRiskDailyBasicService.getById(id))
                .map(StyleRiskDailyPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(StyleRiskDailyBO value) {
        return styleRiskDailyBasicService.create(StyleRiskDailyPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(StyleRiskDailyBO value) {
        return styleRiskDailyBasicService.update(StyleRiskDailyPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return styleRiskDailyBasicService.delete(id);
    }
}
