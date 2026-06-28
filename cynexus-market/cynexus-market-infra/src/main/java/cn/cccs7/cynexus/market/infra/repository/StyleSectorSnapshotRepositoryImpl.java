package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.StyleSectorSnapshotBO;
import cn.cccs7.cynexus.market.domain.repository.StyleSectorSnapshotRepository;
import cn.cccs7.cynexus.market.infra.basic.service.StyleSectorSnapshotBasicService;
import cn.cccs7.cynexus.market.infra.convert.StyleSectorSnapshotPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class StyleSectorSnapshotRepositoryImpl implements StyleSectorSnapshotRepository {

    private final StyleSectorSnapshotBasicService styleSectorSnapshotBasicService;

    public StyleSectorSnapshotRepositoryImpl(StyleSectorSnapshotBasicService styleSectorSnapshotBasicService) {
        this.styleSectorSnapshotBasicService = styleSectorSnapshotBasicService;
    }

    @Override
    public List<StyleSectorSnapshotBO> list() {
        return StyleSectorSnapshotPOConverter.INSTANCE.toTargetList(styleSectorSnapshotBasicService.listAll());
    }

    @Override
    public StyleSectorSnapshotBO findById(Long id) {
        return Optional.ofNullable(styleSectorSnapshotBasicService.getById(id))
                .map(StyleSectorSnapshotPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(StyleSectorSnapshotBO value) {
        return styleSectorSnapshotBasicService.create(StyleSectorSnapshotPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(StyleSectorSnapshotBO value) {
        return styleSectorSnapshotBasicService.update(StyleSectorSnapshotPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return styleSectorSnapshotBasicService.delete(id);
    }
}
