package cn.cccs7.cynexus.market.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.cccs7.cynexus.market.domain.entity.SectorRankBO;
import cn.cccs7.cynexus.market.domain.service.sector.SectorRankQueryDomainService;
import cn.cccs7.cynexus.market.domain.valueobject.Sector;
import cn.cccs7.cynexus.market.domain.valueobject.SectorStrength;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class SectorRankAppServiceTest {

    @Test
    void sectorRankDelegatesToPersistedSnapshotRepository() {
        SectorRankQueryDomainService domainService = mock(SectorRankQueryDomainService.class);
        when(domainService.getSectorRank("2026-07-21", 5)).thenReturn(List.of(SectorRankBO.builder()
                .sector(new Sector("881121", "半导体"))
                .strength(SectorStrength.builder().changePercent(new BigDecimal("9.83")).mainNetInflow(new BigDecimal("216.04")).build())
                .build()));

        var result = new SectorRankAppService(domainService).getSectorRank("2026-07-21", 5);

        assertEquals("半导体", result.get(0).getSector().getSectorName());
        assertEquals(new BigDecimal("216.04"), result.get(0).getStrength().getMainNetInflow());
    }
}
