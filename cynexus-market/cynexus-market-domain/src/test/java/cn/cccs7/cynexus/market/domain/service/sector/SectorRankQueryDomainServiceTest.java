package cn.cccs7.cynexus.market.domain.service.sector;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.cccs7.cynexus.market.domain.entity.SectorRankBO;
import cn.cccs7.cynexus.market.domain.repository.SectorRankRepository;
import cn.cccs7.cynexus.market.domain.service.sector.impl.SectorRankQueryDomainServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;

class SectorRankQueryDomainServiceTest {

    @Test
    void clampsRankLimit() {
        int[] captured = new int[1];
        SectorRankRepository repository = (tradeDate, limit) -> {
            captured[0] = limit;
            return List.of();
        };

        new SectorRankQueryDomainServiceImpl(repository).getSectorRank("2026-07-23", 999);

        assertEquals(100, captured[0]);
    }
}
