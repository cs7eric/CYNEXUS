package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.SelfSelectedListBO;
import cn.cccs7.cynexus.market.domain.repository.SelfSelectedListRepository;
import cn.cccs7.cynexus.market.infra.basic.service.SelfSelectedListBasicService;
import cn.cccs7.cynexus.market.infra.convert.SelfSelectedListPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class SelfSelectedListRepositoryImpl implements SelfSelectedListRepository {

    private final SelfSelectedListBasicService basicService;

    public SelfSelectedListRepositoryImpl(SelfSelectedListBasicService basicService) {
        this.basicService = basicService;
    }

    @Override
    public List<SelfSelectedListBO> listAll() {
        return SelfSelectedListPOConverter.INSTANCE.toTargetList(basicService.listAll());
    }

    @Override
    public SelfSelectedListBO findById(Long id) {
        return Optional.ofNullable(basicService.getById(id))
                .map(SelfSelectedListPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(SelfSelectedListBO value) {
        return basicService.create(SelfSelectedListPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(SelfSelectedListBO value) {
        return basicService.update(SelfSelectedListPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return basicService.delete(id);
    }
}
