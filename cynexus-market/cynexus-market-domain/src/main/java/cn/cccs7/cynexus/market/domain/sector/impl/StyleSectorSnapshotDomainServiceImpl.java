package cn.cccs7.cynexus.market.domain.sector.impl;

import cn.cccs7.cynexus.market.domain.entity.StyleSectorSnapshotBO;
import cn.cccs7.cynexus.market.domain.repository.StyleSectorSnapshotRepository;
import cn.cccs7.cynexus.market.domain.sector.StyleSectorSnapshotDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StyleSectorSnapshotDomainServiceImpl implements StyleSectorSnapshotDomainService {

    private final StyleSectorSnapshotRepository styleSectorSnapshotRepository;

    public StyleSectorSnapshotDomainServiceImpl(StyleSectorSnapshotRepository styleSectorSnapshotRepository) {
        this.styleSectorSnapshotRepository = styleSectorSnapshotRepository;
    }

    @Override
    public List<StyleSectorSnapshotBO> list() {
        return styleSectorSnapshotRepository.list();
    }

    @Override
    public StyleSectorSnapshotBO findById(Long id) {
        return styleSectorSnapshotRepository.findById(id);
    }

    @Override
    public Long create(StyleSectorSnapshotBO value) {
        return styleSectorSnapshotRepository.create(value);
    }

    @Override
    public boolean update(StyleSectorSnapshotBO value) {
        return styleSectorSnapshotRepository.update(value);
    }

    @Override
    public boolean delete(Long id) {
        return styleSectorSnapshotRepository.delete(id);
    }
}
