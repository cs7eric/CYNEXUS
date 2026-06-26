package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.SelfSelectedListItemBO;
import cn.cccs7.cynexus.market.domain.repository.SelfSelectedListItemRepository;
import cn.cccs7.cynexus.market.infra.basic.service.SelfSelectedListItemBasicService;
import cn.cccs7.cynexus.market.infra.convert.SelfSelectedListItemPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class SelfSelectedListItemRepositoryImpl implements SelfSelectedListItemRepository {

    private final SelfSelectedListItemBasicService basicService;

    public SelfSelectedListItemRepositoryImpl(SelfSelectedListItemBasicService basicService) {
        this.basicService = basicService;
    }

    @Override
    public List<SelfSelectedListItemBO> listAll() {
        return SelfSelectedListItemPOConverter.INSTANCE.toTargetList(basicService.listAll());
    }

    @Override
    public SelfSelectedListItemBO findById(Long id) {
        return Optional.ofNullable(basicService.getById(id))
                .map(SelfSelectedListItemPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(SelfSelectedListItemBO value) {
        return basicService.create(SelfSelectedListItemPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(SelfSelectedListItemBO value) {
        return basicService.update(SelfSelectedListItemPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return basicService.delete(id);
    }
}
