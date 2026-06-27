package cn.cccs7.cynexus.market.domain.service.impl;

import cn.cccs7.cynexus.market.domain.entity.MaCountDailyBO;
import cn.cccs7.cynexus.market.domain.repository.MaCountDailyRepository;
import cn.cccs7.cynexus.market.domain.service.MaCountDailyDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MaCountDailyDomainServiceImpl implements MaCountDailyDomainService {

    private final MaCountDailyRepository maCountDailyRepository;

    public MaCountDailyDomainServiceImpl(MaCountDailyRepository maCountDailyRepository) {
        this.maCountDailyRepository = maCountDailyRepository;
    }

    @Override
    public List<MaCountDailyBO> list() {
        return maCountDailyRepository.list();
    }

    @Override
    public MaCountDailyBO findById(Long id) {
        return maCountDailyRepository.findById(id);
    }

    @Override
    public Long create(MaCountDailyBO value) {
        return maCountDailyRepository.create(value);
    }

    @Override
    public boolean update(MaCountDailyBO value) {
        return maCountDailyRepository.update(value);
    }

    @Override
    public boolean delete(Long id) {
        return maCountDailyRepository.delete(id);
    }
}
