package cn.cccs7.cynexus.market.domain.sentiment.impl;

import cn.cccs7.cynexus.market.domain.entity.StyleRiskDailyBO;
import cn.cccs7.cynexus.market.domain.repository.StyleRiskDailyRepository;
import cn.cccs7.cynexus.market.domain.sentiment.StyleRiskDailyDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StyleRiskDailyDomainServiceImpl implements StyleRiskDailyDomainService {

    private final StyleRiskDailyRepository styleRiskDailyRepository;

    public StyleRiskDailyDomainServiceImpl(StyleRiskDailyRepository styleRiskDailyRepository) {
        this.styleRiskDailyRepository = styleRiskDailyRepository;
    }

    @Override
    public List<StyleRiskDailyBO> list() {
        return styleRiskDailyRepository.list();
    }

    @Override
    public StyleRiskDailyBO findById(Long id) {
        return styleRiskDailyRepository.findById(id);
    }

    @Override
    public Long create(StyleRiskDailyBO value) {
        return styleRiskDailyRepository.create(value);
    }

    @Override
    public boolean update(StyleRiskDailyBO value) {
        return styleRiskDailyRepository.update(value);
    }

    @Override
    public boolean delete(Long id) {
        return styleRiskDailyRepository.delete(id);
    }
}
