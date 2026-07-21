package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.SectorRankBO;
import cn.cccs7.cynexus.market.domain.repository.SectorRankRepository;
import cn.cccs7.cynexus.market.domain.valueobject.Sector;
import cn.cccs7.cynexus.market.domain.valueobject.SectorStrength;
import cn.cccs7.cynexus.market.infra.basic.entity.SectorFundFlowDailySnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.service.SectorFundFlowDailySnapshotBasicService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class SectorRankRepositoryImpl implements SectorRankRepository {

    private final SectorFundFlowDailySnapshotBasicService snapshotService;

    public SectorRankRepositoryImpl(SectorFundFlowDailySnapshotBasicService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @Override
    public List<SectorRankBO> findByTradeDate(String tradeDate, int limit) {
        if (tradeDate == null || tradeDate.isBlank()) return List.of();
        LocalDate date = LocalDate.parse(tradeDate);
        return snapshotService.listByTradeDate("industry", date, Math.max(limit, 1)).stream()
                .map(this::toBO)
                .toList();
    }

    private SectorRankBO toBO(SectorFundFlowDailySnapshotPO row) {
        return SectorRankBO.builder()
                .sector(new Sector(row.getSectorCode(), row.getSectorName()))
                .strength(SectorStrength.builder()
                        .changePercent(row.getChangePct())
                        .turnoverAmount(null)
                        .mainNetInflow(row.getNet())
                        .build())
                .build();
    }
}
